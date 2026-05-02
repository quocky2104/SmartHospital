package com.example.SmartHospital.model;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDateTime;

import com.example.SmartHospital.enums.IssueStatus;
import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "issues")
@Data
public class Issue {
    @Id
    private String id;

    @Column(nullable = false)
    private String reporterId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.OPEN;

    @Column(nullable = true, length = 1000)
    private String adminResponse;

    @ManyToOne
    @JoinColumn(name = "assigned_admin_id", nullable = true)
    private Admin assignedAdmin;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime resolvedAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = CustomIdGenerator.generateIssueId();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
