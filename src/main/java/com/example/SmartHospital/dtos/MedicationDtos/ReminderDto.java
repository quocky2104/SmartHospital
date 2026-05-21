package com.example.SmartHospital.dtos.MedicationDtos;

import java.time.LocalDateTime;
import java.util.List;
import com.example.SmartHospital.enums.ReminderType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.SmartHospital.model.Reminder;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReminderDto {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String id;
    private String medicationId;
    private LocalDateTime time;
    private ReminderType scheduleType;
    private String timeOfDay;
    private String dayOfWeek;
    private List<String> timesOfDay;
    private Boolean notifyEmail;
    private Boolean notifySystem;
    private Boolean taken;

    public ReminderDto(Reminder r) {
        this.id = r.getId();
        this.medicationId = r.getMedicationId();
        this.time = r.getTime();
        this.scheduleType = r.getScheduleType();
        this.timeOfDay = r.getTimeOfDay();
        this.dayOfWeek = r.getDayOfWeek();
        this.timesOfDay = readTimesOfDay(r.getTimesOfDayJson());
        this.notifyEmail = r.getNotifyEmail();
        this.notifySystem = r.getNotifySystem();
        this.taken = r.getTaken();
    }

    private static List<String> readTimesOfDay(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        try {
            return OBJECT_MAPPER.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }
}
