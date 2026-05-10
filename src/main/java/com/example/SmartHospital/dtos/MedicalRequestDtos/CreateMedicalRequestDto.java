package com.example.SmartHospital.dtos.MedicalRequestDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMedicalRequestDto {

    @NotBlank
    private String type;

    @NotBlank
    private String subject;

    @NotBlank
    private String description;

    @NotBlank
    private String priority;
}
