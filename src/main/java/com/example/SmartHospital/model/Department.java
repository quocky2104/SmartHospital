package com.example.SmartHospital.model;
import org.hibernate.annotations.ColumnDefault;

import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "department")
public class Department {
    @Id
    private String id;

    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generateDepartmentId();
        }
    }

    @Column(unique = true)
    private String name;

    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean isDeleted = false;
}
