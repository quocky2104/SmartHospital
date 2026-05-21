package com.example.SmartHospital.dtos.MedicationDtos;

import java.util.List;
import com.example.SmartHospital.enums.ReminderType;
import lombok.Data;

@Data
public class CreateReminderRequest {
    private ReminderType scheduleType; // once, weekly, multiple_daily
    private String time; // ISO datetime for once schedule, or local time for daily
    private String dayOfWeek; // MONDAY..SUNDAY for weekly schedule
    private List<String> timesOfDay; // list of local times for multiple_daily
    private Boolean notifyEmail = false;
    private Boolean notifySystem = false;
}
