package com.example.SmartHospital.service.notification;

import java.util.List;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.config.RabbitMQConfig;
import com.example.SmartHospital.dtos.NotificationDtos.AppointmentNotificationPayload;
import com.example.SmartHospital.dtos.NotificationDtos.NotificationResponse;
import com.example.SmartHospital.model.Appointment;
import com.example.SmartHospital.model.Notification;
import com.example.SmartHospital.repository.NotificationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final INotificationSender notificationSender;
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    private void sendAppointmentEvent(
        String recipientUserId,
        String recipientEmail,
        String recipientName,
        Appointment appointment,
        String eventType,
        String message
    ) {
        AppointmentNotificationPayload payload = new AppointmentNotificationPayload(
            eventType,
            appointment.getId(),
            appointment.getPatient().getId(),
            appointment.getPatient().getFullName(),
            appointment.getDoctor().getId(),
            appointment.getAppointmentDateTime(),
            message
        );

        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setType(eventType);
        notification.setTitle("Appointment update");
        notification.setMessage(message);
        notification.setAppointmentId(appointment.getId());
        notification.setIsRead(false);
        notificationRepository.save(notification);

        notificationSender.sendAppointmentNotification(recipientUserId, recipientEmail, recipientName, payload);
    }

    @Transactional
    public void notifyDoctorAppointmentEvent(Appointment appointment, String eventType, String message) {
        String doctorId = appointment.getDoctor().getId();
        sendAppointmentEvent(
            doctorId,
            appointment.getDoctor().getEmail(),
            appointment.getDoctor().getFullName(),
            appointment,
            eventType,
            message
        );
        publishRabbitIfAvailable(doctorId, buildPayload(appointment, eventType, message));
    }

    @Transactional
    public void notifyPatientAppointmentEvent(Appointment appointment, String eventType, String message) {
        String patientId = appointment.getPatient().getId();
        sendAppointmentEvent(
            patientId,
            appointment.getPatient().getEmail(),
            appointment.getPatient().getFullName(),
            appointment,
            eventType,
            message
        );
    }

    public List<NotificationResponse> getMyNotifications(String userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(NotificationResponse::new)
            .toList();
    }

    @Transactional
    public boolean markAsRead(String userId, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || !notification.getRecipientUserId().equals(userId)) {
            return false;
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
        return true;
    }

    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> notifications = notificationRepository.findByRecipientUserIdAndIsReadFalse(userId);
        notifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void markAsUnread(String userId, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || !notification.getRecipientUserId().equals(userId)) {
            return;
        }
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientUserIdAndIsReadFalse(userId);
    }

    private AppointmentNotificationPayload buildPayload(Appointment appointment, String eventType, String message) {
        return new AppointmentNotificationPayload(
            eventType,
            appointment.getId(),
            appointment.getPatient().getId(),
            appointment.getPatient().getFullName(),
            appointment.getDoctor().getId(),
            appointment.getAppointmentDateTime(),
            message
        );
    }

    private void publishRabbitIfAvailable(String doctorId, AppointmentNotificationPayload payload) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.APPOINTMENT_EXCHANGE, "doctor." + doctorId, payload);
        } catch (AmqpException ex) {
            log.warn("RabbitMQ not available, websocket delivery still succeeded: {}", ex.getMessage());
        }
    }
}