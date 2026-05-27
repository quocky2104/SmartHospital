package com.example.SmartHospital.model;

import java.time.LocalDateTime;

import com.example.SmartHospital.enums.RequestStatus;
import com.example.SmartHospital.enums.RequestType;
import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name="request")
public class Request {
    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private RequestType type;

    @Column(nullable=false)
    private String rawText;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private RequestStatus status;

    @Column(nullable=false)
    private LocalDateTime reportedAt;

    @ManyToOne(optional=false)
    @JoinColumn(name="patient_id")
    private Patient patient;

    @ManyToOne(optional=false)
    @JoinColumn(name="handler_id")
    private User handler;

    @ManyToOne(optional=false)
    @JoinColumn(name="department_id")
    private Department department;

    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generateRequestId();
        }
        this.reportedAt = LocalDateTime.now();
    }
    @PreUpdate
    public void validateHandlerType() {
        if (!(this.handler instanceof Doctor || this.handler instanceof Admin)) {
            throw new IllegalStateException(
                "Invalid handler type"
            );
        }
    }
}
