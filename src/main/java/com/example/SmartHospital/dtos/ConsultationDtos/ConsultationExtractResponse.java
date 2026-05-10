package com.example.SmartHospital.dtos.ConsultationDtos;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultationExtractResponse {

    @JsonProperty("main_symptoms")
    private List<String> main_symptoms = new ArrayList<>();

    private String duration;

    @JsonProperty("additional_signs")
    private List<String> additional_signs = new ArrayList<>();

    private String location;

    @JsonProperty("symptom_character")
    private String symptom_character;

    @JsonProperty("aggravating_factors")
    private List<String> aggravating_factors = new ArrayList<>();

    @JsonProperty("relieving_factors")
    private List<String> relieving_factors = new ArrayList<>();

    private String progression;

    @JsonProperty("red_flags")
    private List<String> red_flags = new ArrayList<>();

    @JsonProperty("raw_text")
    private String raw_text;
}
