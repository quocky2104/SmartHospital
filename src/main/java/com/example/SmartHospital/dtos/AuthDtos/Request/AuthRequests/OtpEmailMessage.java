package com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpEmailMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String token;
    private String email;
    private String fullName;
    private String otp;
}