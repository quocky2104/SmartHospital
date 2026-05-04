package com.example.SmartHospital.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "doctor")
@EqualsAndHashCode(callSuper = false)
public class Doctor extends User {
    @Column(nullable=false)
    private String workingHours; //Ex: "09:00-12:00,13:00-17:00"
    @Column(nullable=false)
    private String availabilityStatus;

    @OneToMany(mappedBy = "doctor")
    private List<Appointment> appointments;

    @ManyToOne(optional=false)
    @JoinColumn(name="department_id")
    private Department department;

}