package com.example.SmartHospital.dtos.MedicalRequestDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRequestResponse {

    private String id;

    private String type;

    private String subject;

    private String description;

    private String patientId;

    private String createdAt;

    private String updatedAt;

    private String response;
}
