package com.example.SmartHospital.dtos.NotificationDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailNotificationPayload {
    private String recipientEmail;
    private String subject;
    private String body;
}
