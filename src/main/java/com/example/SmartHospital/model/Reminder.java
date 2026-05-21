package com.example.SmartHospital.model;

import java.time.LocalDateTime;

import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import com.example.SmartHospital.enums.ReminderType;

@Entity
@Data
@Table(name = "reminder")
public class Reminder {
    @Id
    private String id;

    @PrePersist
    public void ensureId() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generateReminderId();
        }
    }

    @Column(nullable = false)
    private String medicationId;

    private LocalDateTime time;
    private ReminderType scheduleType; // once, weekly, multiple_daily
    private String timeOfDay;
    private String dayOfWeek;
    private String timesOfDayJson;
    private Boolean notifyEmail = false;
    private Boolean notifySystem = false;
    private Boolean taken = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;
}