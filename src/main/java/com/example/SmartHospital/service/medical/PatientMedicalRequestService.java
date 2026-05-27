package com.example.SmartHospital.service.medical;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.SmartHospital.dtos.MedicalRequestDtos.CreateMedicalRequestDto;
import com.example.SmartHospital.dtos.MedicalRequestDtos.MedicalRequestResponse;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.model.PatientMedicalRequest;
import com.example.SmartHospital.repository.AppointmentRepository;
import com.example.SmartHospital.repository.PatientMedicalRequestRepository;
import com.example.SmartHospital.repository.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientMedicalRequestService {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String OPEN_STATUS = "open";
    private static final String CLOSED_STATUS = "closed";

    private final PatientMedicalRequestRepository repository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public MedicalRequestResponse create(String patientUserId, CreateMedicalRequestDto dto) {
        Patient patient = patientRepository.findById(patientUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient not found"));

        PatientMedicalRequest entity = new PatientMedicalRequest();
        entity.setPatient(patient);
        entity.setSubject(dto.getSubject());
        entity.setDescription(dto.getDescription());
        entity.setStatus(OPEN_STATUS);

        return toResponse(repository.save(entity));
    }

    public List<MedicalRequestResponse> list(Authentication authentication) {
        String userId = authentication.getName();
        boolean doctor = authentication.getAuthorities().stream()
            .anyMatch(a -> "ROLE_DOCTOR".equals(a.getAuthority()));

        List<PatientMedicalRequest> rows = doctor
            ? repository.findByDoctorAppointments(userId)
            : repository.findByPatient_IdOrderByCreatedAtDesc(userId);

        return rows.stream().map(this::toResponse).toList();
    }

    public MedicalRequestResponse getById(String id, Authentication authentication) {
        PatientMedicalRequest entity = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        boolean doctor = authentication.getAuthorities().stream()
            .anyMatch(a -> "ROLE_DOCTOR".equals(a.getAuthority()));
            
        if (doctor) {
            if (!appointmentRepository.existsByPatient_IdAndDoctor_Id(entity.getPatient().getId(), authentication.getName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: No appointment with this patient");
            }
        } else if (!entity.getPatient().getId().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return toResponse(entity);
    }

    public MedicalRequestResponse closeRequest(String id, Authentication authentication) {
        PatientMedicalRequest entity = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!entity.getPatient().getId().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        entity.setStatus(CLOSED_STATUS);
        return toResponse(repository.save(entity));
    }

    public MedicalRequestResponse reopenRequest(String id, Authentication authentication) {
        PatientMedicalRequest entity = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!entity.getPatient().getId().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        entity.setStatus(OPEN_STATUS);
        return toResponse(repository.save(entity));
    }

    private MedicalRequestResponse toResponse(PatientMedicalRequest e) {
        MedicalRequestResponse response = new MedicalRequestResponse();
        response.setId(e.getId());
        response.setSubject(e.getSubject());
        response.setDescription(e.getDescription());
        response.setPatientId(e.getPatient().getId());
        response.setPatientName(e.getPatient().getFullName());
        response.setCreatedAt(e.getCreatedAt() != null ? ISO_LOCAL.format(e.getCreatedAt()) : null);
        response.setUpdatedAt(e.getUpdatedAt() != null ? ISO_LOCAL.format(e.getUpdatedAt()) : null);
        response.setStatus(normalizeStatus(e.getStatus()));
        response.setResponse(null);
        return response;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return OPEN_STATUS;
        }

        String normalized = status.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return OPEN_STATUS;
        }

        return switch (normalized) {
            case "completed", "closed", "resolved", "done" -> CLOSED_STATUS;
            default -> OPEN_STATUS;
        };
    }
}
