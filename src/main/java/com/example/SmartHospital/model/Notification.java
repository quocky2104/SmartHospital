package com.example.SmartHospital.model;

import java.time.LocalDateTime;
import com.example.SmartHospital.helper.CustomIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "notification")
@Data
public class Notification {
    @Id
    private String id;

    @Column(nullable = false)
    private String recipientUserId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = true)
    private String appointmentId;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = CustomIdGenerator.generateNotificationId();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}