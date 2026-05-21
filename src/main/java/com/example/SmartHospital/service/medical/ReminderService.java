package com.example.SmartHospital.service.medical;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.MedicationDtos.CreateReminderRequest;
import com.example.SmartHospital.dtos.MedicationDtos.ReminderDto;
import com.example.SmartHospital.model.Reminder;
import com.example.SmartHospital.repository.MedicationRepository;
import com.example.SmartHospital.repository.ReminderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.SmartHospital.enums.ReminderType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final ObjectMapper objectMapper;
    private final ReminderRepository reminderRepository;
    private final MedicationRepository medicationRepository;

    public List<ReminderDto> findByMedicationId(String medId) {
        return reminderRepository.findByMedicationIdAndIsDeletedFalse(medId).stream().map(ReminderDto::new).toList();
    }

    public ReminderDto create(String medId, CreateReminderRequest req) {
        medicationRepository.findByIdAndIsDeletedFalse(medId)
            .orElseThrow(() -> new IllegalArgumentException("Medication not found"));
        Reminder reminder = new Reminder();
        reminder.setMedicationId(medId);
        ReminderType scheduleType = req.getScheduleType() == null
            ? ReminderType.ONCE
            : ReminderType.valueOf(req.getScheduleType().name().toUpperCase());
        reminder.setScheduleType(scheduleType);
        applySchedule(reminder, req);
        reminder.setNotifyEmail(Boolean.TRUE.equals(req.getNotifyEmail()));
        reminder.setNotifySystem(Boolean.TRUE.equals(req.getNotifySystem()));
        reminder.setCreatedAt(LocalDateTime.now());
        reminder.setUpdatedAt(LocalDateTime.now());

        Reminder saved = reminderRepository.save(reminder);
        return new ReminderDto(saved);
    }

    public void delete(String reminderId) {
        Reminder r = reminderRepository.findById(reminderId).orElse(null);
        if (r == null) return;
        r.setIsDeleted(true);
        r.setUpdatedAt(LocalDateTime.now());
        reminderRepository.save(r);
    }

    public List<ReminderDto> findAll() {
        return reminderRepository.findAll().stream()
            .filter(r -> r.getIsDeleted() == null || !r.getIsDeleted())
            .map(ReminderDto::new)
            .toList();
    }

    private void applySchedule(Reminder reminder, CreateReminderRequest req) {
        ReminderType scheduleType = reminder.getScheduleType();
        if (ReminderType.MULTIPLE_DAILY == scheduleType) {
            List<String> times = req.getTimesOfDay() == null ? List.of() : req.getTimesOfDay();
            reminder.setTimesOfDayJson(writeTimesOfDay(times));
            if (!times.isEmpty()) {
                reminder.setTimeOfDay(times.get(0));
            }
            return;
        }

        if (req.getTime() != null && !req.getTime().isBlank()) {
            if (ReminderType.ONCE == scheduleType) {
                reminder.setTime(parseTime(req.getTime()));
            }
            reminder.setTimeOfDay(req.getTime());
        }

        if (ReminderType.WEEKLY == scheduleType) {
            reminder.setDayOfWeek(req.getDayOfWeek());
        }
    }

    private LocalDateTime parseTime(String value) {
        return LocalDateTime.parse(value);
    }

    private String writeTimesOfDay(List<String> times) {
        try {
            return objectMapper.writeValueAsString(new ArrayList<>(times));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize reminder times", ex);
        }
    }
}
