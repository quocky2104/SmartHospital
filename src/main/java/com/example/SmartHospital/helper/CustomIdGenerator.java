package com.example.SmartHospital.helper;

import java.util.UUID;


public class CustomIdGenerator {

    private CustomIdGenerator() {
        // Private constructor to prevent instantiation
    }

    public static String generateTestOrderId() {
        return "TO-" + randomShortUUID();
    }

    public static String generateTestOrderResultId() {
        return "TR-" + randomShortUUID();
    }

    public static String generateCommentId() {
        return "CMT-" + randomShortUUID();
    }

    public static String generateMedicalRecordId() {
        return "MR-" + randomShortUUID();
    }

    public static String generateUserId() {
        return "U-" + randomShortUUID();
    }

    public static String generateRequestId() {
        return "RQ-" + randomShortUUID();
    }

    public static String generatePrescriptionId() {
        return "PR-" + randomShortUUID();
    }

    public static String generateMedicationId() {
        return "M-" + randomShortUUID();
    }

    public static String generateDepartmentId() {
        return "DPT-" + randomShortUUID();
    }

    public static String generateAppointmentId() {
        return "AP-" + randomShortUUID();
    }

    public static String generateDoctorPatientMessageId() {
        return "DPM-" + randomShortUUID();
    }

    public static String generatePatientChatbotMessageId() {
        return "PCM-" + randomShortUUID();
    }

    public static String generateDoctorChatbotMessageId() {
        return "DCM-" + randomShortUUID();
    }

    public static String generateNotificationId() {
        return "NTF-" + randomShortUUID();
    }

    public static String generateIssueId() {
        return "ISS-" + randomShortUUID();
    }

    // Generate a random short UUID
    private static String randomShortUUID() {
        return UUID.randomUUID().toString().split("-")[0].toUpperCase();
    }
}
