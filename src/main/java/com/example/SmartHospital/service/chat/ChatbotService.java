package com.example.SmartHospital.service.chat;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.config.exceptions.BadRequestException;
import com.example.SmartHospital.model.Appointment;
import com.example.SmartHospital.model.PatientMedicalRequest;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.AppointmentRepository;
import com.example.SmartHospital.repository.PatientMedicalRequestRepository;
import com.example.SmartHospital.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatbotService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientMedicalRequestRepository requestRepository;

    private static final String GEMINI_GENERATE_PATH = "/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String GEMINI_HOST = "generativelanguage.googleapis.com";

    @Value("${gemini.api-key:}")
    private String apiKey;

    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    public String getChatbotResponse(String userId, String userMessage) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new BadRequestException("Gemini API key is not configured on the server.");
            }

            User user = userRepository.findById(userId).orElse(null);
            String contextData = buildContextData(user);

            String systemPrompt = """
                You are the SMARTMED AI assistant, a helpful hospital chatbot.
                You help users understand how to use the system, check their appointments, and view their medical requests.
                
                Current Date/Time: %s
                Hospital Working Hours: 07:00 - 18:00 (7 AM - 6 PM)
                
                USER INFORMATION:
                %s
                
                INSTRUCTIONS:
                1. Answer the user's query clearly, politely, and concisely.
                2. If the user asks about their own appointments or requests, use the provided USER INFORMATION to answer them accurately.
                3. If the user asks about general hospital info (like working hours), inform them it is from 7 AM to 6 PM.
                4. If the user asks what the "Request" function is or how it works, explain it based on this system knowledge:
                   - The Request (or Consultation Triage) function is an intelligent feature where patients can write a free-text description of their symptoms or health issues.
                   - Instead of just sending raw text to doctors, the AI system automatically analyzes and breaks down the text into structured medical fields (like main symptoms, duration, location, aggravating factors, and red flags).
                   - This structured breakdown is then matched with and sent to a doctor, helping the doctor quickly understand the patient's condition before the appointment.
                5. If the user asks about features on the sidebar, guide them:
                   - Dashboard: Overview of your recent activities.
                   - Appointments: View your schedule or book a new appointment.
                   - Requests: Submit a new consultation request using our AI symptom analysis.
                   - Medical Records: View your past medical history.
                   - Report Issue: Report technical problems or bugs.
                   - Profile: Manage your personal details and settings.
                   - Community Posts: Read health tips, engage with other users, and view public discussions.
                6. Do not mention that you are reading from a database, context string, or prompt. Just answer naturally as the system assistant.
                """.formatted(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), contextData);

            ObjectNode root = objectMapper.createObjectNode();

            // System Instruction
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            ArrayNode sysParts = objectMapper.createArrayNode();
            sysParts.add(objectMapper.createObjectNode().put("text", systemPrompt));
            systemInstruction.set("parts", sysParts);
            root.set("systemInstruction", systemInstruction);

            // User Turn
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userTurn = objectMapper.createObjectNode();
            userTurn.put("role", "user");
            ArrayNode userParts = objectMapper.createArrayNode();
            userParts.add(objectMapper.createObjectNode().put("text", userMessage));
            userTurn.set("parts", userParts);
            contents.add(userTurn);
            root.set("contents", contents);

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
            if (textNode.isMissingNode() || textNode.isNull()) {
                return "I'm sorry, I couldn't generate a response at this time.";
            }

            return textNode.asText();

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Chatbot failed", e);
            throw new BadRequestException("Failed to get chatbot response: " + e.getMessage() + " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "unknown"));
        }
    }

    private String buildContextData(User user) {
        if (user == null) {
            return "User not found or unauthenticated.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(user.getFullName()).append("\n");
        sb.append("Role: ").append(user.getRole()).append("\n");

        if (String.valueOf(user.getRole()).equalsIgnoreCase("PATIENT")) {
            Page<Appointment> apptsPage = appointmentRepository.searchAppointmentsForPatient(user.getId(), "", PageRequest.of(0, 5));
            sb.append("\nRecent/Upcoming Appointments (up to 5):\n");
            if (apptsPage.isEmpty()) {
                sb.append("None.\n");
            } else {
                for (Appointment a : apptsPage.getContent()) {
                    String docName = a.getDoctor() != null ? a.getDoctor().getFullName() : "Unassigned";
                    sb.append(String.format("- ID: %s, Date: %s, Status: %s, Doctor: %s\n", 
                        a.getId(), a.getAppointmentDateTime(), a.getStatus(), docName));
                }
            }

            List<PatientMedicalRequest> requests = requestRepository.findByPatient_IdOrderByCreatedAtDesc(user.getId());
            sb.append("\nRecent Requests (up to 3):\n");
            if (requests.isEmpty()) {
                sb.append("None.\n");
            } else {
                int count = 0;
                for (PatientMedicalRequest r : requests) {
                    if (count >= 3) break;
                    sb.append(String.format("- Subject: %s, Status: %s\n", 
                        r.getSubject(), r.getStatus()));
                    count++;
                }
            }
        } else if (String.valueOf(user.getRole()).equalsIgnoreCase("DOCTOR")) {
            Page<Appointment> apptsPage = appointmentRepository.searchAppointmentsForDoctor(user.getId(), "", PageRequest.of(0, 5));
            sb.append("\nRecent/Upcoming Appointments (up to 5):\n");
            if (apptsPage.isEmpty()) {
                sb.append("None.\n");
            } else {
                for (Appointment a : apptsPage.getContent()) {
                    String patName = a.getPatient() != null ? a.getPatient().getFullName() : "Unassigned";
                    sb.append(String.format("- ID: %s, Date: %s, Status: %s, Patient: %s\n", 
                        a.getId(), a.getAppointmentDateTime(), a.getStatus(), patName));
                }
            }
        }
        return sb.toString();
    }
}
