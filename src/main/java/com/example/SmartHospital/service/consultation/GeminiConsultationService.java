package com.example.SmartHospital.service.consultation;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.config.exceptions.BadRequestException;
import com.example.SmartHospital.dtos.ConsultationDtos.ConsultationExtractResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeminiConsultationService {

    /** Local mapper: Spring Boot 4 may not expose an {@link ObjectMapper} bean by default. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_GENERATE_PATH =
        "/v1beta/models/gemini-2.5-flash:generateContent";

    private static final String GEMINI_HOST = "generativelanguage.googleapis.com";

    private static final String SYSTEM_PROMPT = """
        You are a clinical documentation assistant for a hospital triage intake form.

        Your task: read the patient's free-text description of how they feel and extract structured fields ONLY. Output must follow the rules below.

        CRITICAL RULES:
        1) NEVER provide a disease name or diagnosis (no ICD labels, no "you have X"). Do not label conditions.
        2) You MAY use reasonable clinical inference to fill missing fields when the text strongly implies them (e.g. "bad cough for a week" → location may be inferred as throat/respiratory; duration "about one week").
        3) Arrays must be JSON arrays of strings; use [] when unknown or not mentioned.
        4) Use null for optional string fields when unknown or not inferable; never guess wildly.
        5) "raw_text" MUST contain the user's message verbatim (exactly as provided), with only whitespace normalization at most — preserve meaning and wording.

        Output JSON object with EXACTLY these keys:
        - main_symptoms: string[] — primary complaints/symptoms in short phrases.
        - duration: string | null — how long symptoms have lasted if stated or clearly inferable.
        - additional_signs: string[] — other associated signs/symptoms.
        - location: string | null — body region / system if stated or safely inferable from symptoms.
        - symptom_character: string | null — quality of symptom (e.g. sharp, dull, burning) if present.
        - aggravating_factors: string[] — what worsens symptoms.
        - relieving_factors: string[] — what improves symptoms.
        - progression: string | null — getting better/worse/stable if stated or inferable.
        - red_flags: string[] — alarming features explicitly mentioned (breathlessness, chest pain, neurological deficits, etc.) — still NO diagnosis.
        - raw_text: string — verbatim user input.

        Respond with JSON only — no markdown fences, no commentary.""";

    private static final String RESPONSE_SCHEMA_JSON = """
        {
          "type": "OBJECT",
          "properties": {
            "main_symptoms": { "type": "ARRAY", "items": { "type": "STRING" } },
            "duration": { "type": "STRING", "nullable": true },
            "additional_signs": { "type": "ARRAY", "items": { "type": "STRING" } },
            "location": { "type": "STRING", "nullable": true },
            "symptom_character": { "type": "STRING", "nullable": true },
            "aggravating_factors": { "type": "ARRAY", "items": { "type": "STRING" } },
            "relieving_factors": { "type": "ARRAY", "items": { "type": "STRING" } },
            "progression": { "type": "STRING", "nullable": true },
            "red_flags": { "type": "ARRAY", "items": { "type": "STRING" } },
            "raw_text": { "type": "STRING" }
          },
          "required": [
            "main_symptoms",
            "duration",
            "additional_signs",
            "location",
            "symptom_character",
            "aggravating_factors",
            "relieving_factors",
            "progression",
            "red_flags",
            "raw_text"
          ]
        }
        """;

    @Value("${gemini.api-key:}")
    private String apiKey;

    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    private JsonNode responseSchemaNode() throws com.fasterxml.jackson.core.JsonProcessingException {
        return objectMapper.readTree(RESPONSE_SCHEMA_JSON);
    }

    public ConsultationExtractResponse analyze(String rawText) {
        String trimmed = rawText == null ? "" : rawText.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Symptom description is required.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("Gemini API key is not configured on the server.");
        }

        try {
            JsonNode schema = responseSchemaNode();
            ObjectNode root = objectMapper.createObjectNode();

            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ArrayNode sysParts = objectMapper.createArrayNode();
            sysParts.add(objectMapper.createObjectNode().put("text", SYSTEM_PROMPT));
            systemInstruction.set("parts", sysParts);
            root.set("systemInstruction", systemInstruction);

            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userTurn = objectMapper.createObjectNode();
            userTurn.put("role", "user");
            ArrayNode userParts = objectMapper.createArrayNode();
            userParts.add(objectMapper.createObjectNode().put("text", trimmed));
            userTurn.set("parts", userParts);
            contents.add(userTurn);
            root.set("contents", contents);

            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.set("responseSchema", schema);
            root.set("generationConfig", generationConfig);

            String payload = objectMapper.writeValueAsString(root);
            String query = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            URI uri = URI.create("https://" + GEMINI_HOST + GEMINI_GENERATE_PATH + "?key=" + query);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

            java.net.http.HttpResponse<String> response =
                httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                JsonNode errBody = objectMapper.readTree(response.body());
                String msg = errBody.path("error").path("message").asText("Gemini request failed");
                throw new BadRequestException(msg);
            }

            JsonNode body = objectMapper.readTree(response.body());
            JsonNode textNode = body.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            String text = textNode.isMissingNode() || textNode.isNull() ? null : textNode.asText();
            if (text == null || text.isBlank()) {
                throw new BadRequestException("No structured response from Gemini.");
            }

            ConsultationExtractResponse parsed = objectMapper.readValue(text, ConsultationExtractResponse.class);
            return normalize(parsed, trimmed);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini consultation analyze failed", e);
            throw new BadRequestException("Could not analyze consultation: " + e.getMessage(), e);
        }
    }

    private ConsultationExtractResponse normalize(ConsultationExtractResponse r, String fallbackRaw) {
        if (r.getMain_symptoms() == null) {
            r.setMain_symptoms(List.of());
        }
        if (r.getAdditional_signs() == null) {
            r.setAdditional_signs(List.of());
        }
        if (r.getAggravating_factors() == null) {
            r.setAggravating_factors(List.of());
        }
        if (r.getRelieving_factors() == null) {
            r.setRelieving_factors(List.of());
        }
        if (r.getRed_flags() == null) {
            r.setRed_flags(List.of());
        }
        if (r.getRaw_text() == null || r.getRaw_text().isBlank()) {
            r.setRaw_text(fallbackRaw);
        }
        return r;
    }
}
