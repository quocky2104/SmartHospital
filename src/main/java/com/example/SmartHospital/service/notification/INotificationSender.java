package com.example.SmartHospital.service.notification;

import com.example.SmartHospital.dtos.NotificationDtos.AppointmentNotificationPayload;

public interface INotificationSender {
    void sendAppointmentAlert(String receiverId, AppointmentNotificationPayload payload);

    void sendAppointmentEmail(String toEmail, String receiverName, AppointmentNotificationPayload payload);

    // This default method sends both WebSocket and email notifications for appointment events
    default void sendAppointmentNotification(
        String receiverId,
        String toEmail,
        String receiverName,
        AppointmentNotificationPayload payload
    ) {
        sendAppointmentAlert(receiverId, payload);
        sendAppointmentEmail(toEmail, receiverName, payload);
    }
}
