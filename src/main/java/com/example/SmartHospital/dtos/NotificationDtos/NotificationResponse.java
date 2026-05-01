package com.example.SmartHospital.dtos.NotificationDtos;

import java.time.LocalDateTime;

import com.example.SmartHospital.model.Notification;

import lombok.Data;

@Data
public class NotificationResponse {
    private String id;
    private String type;
    private String title;
    private String message;
    private String appointmentId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public NotificationResponse(Notification notification) {
        this.id = notification.getId();
        this.type = notification.getType();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.appointmentId = notification.getAppointmentId();
        this.isRead = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
    }
}