package com.example.SmartHospital.service.notification;

import java.util.List;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.config.RabbitMQConfig;
import com.example.SmartHospital.dtos.NotificationDtos.AppointmentNotificationPayload;
import com.example.SmartHospital.dtos.NotificationDtos.EmailNotificationPayload;
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
    private final com.example.SmartHospital.service.messaging.WebSocketMessagingService webSocketMessagingService;

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
        // Send WebSocket notification immediately, so the doctor receives real-time alerts even if RabbitMQ is down
        sendAppointmentEvent(
            doctorId,
            appointment.getDoctor().getEmail(),
            appointment.getDoctor().getFullName(),
            appointment,
            eventType,
            message
        );
        // Attempt to publish to RabbitMQ
        // but if it fails, the WebSocket notification will still have been sent
        // The email will also still be queued via RabbitMQ, so the notification is not lost and retry
        publishRabbitIfAvailable("doctor", doctorId, buildPayload(appointment, eventType, message));
        queueEmailNotification(appointment.getDoctor().getEmail(), "Appointment Update", message);
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
        publishRabbitIfAvailable("patient", patientId, buildPayload(appointment, eventType, message));
        queueEmailNotification(appointment.getPatient().getEmail(), "Appointment Update", message);
    }

    private void queueEmailNotification(String recipientEmail, String subject, String message) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            return;
        }
        try {
            EmailNotificationPayload emailPayload = new EmailNotificationPayload(recipientEmail, subject, message);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY_PATTERN, emailPayload);
        } catch (AmqpException ex) {
            log.warn("Failed to queue email to RabbitMQ: {}", ex.getMessage());
        }
    }

    public List<NotificationResponse> getMyNotifications(String userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(NotificationResponse::new)
            .toList();
    }

    @Transactional
    public void notifyUserEvent(
        String recipientUserId,
        String recipientEmail,
        String recipientName,
        String eventType,
        String title,
        String message,
        String referenceId
    ) {
        com.example.SmartHospital.model.Notification notification = new com.example.SmartHospital.model.Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setType(eventType);
        notification.setTitle(title);
        notification.setMessage(message);
        // store reference id in appointmentId field for compatibility (may be null)
        notification.setAppointmentId(referenceId);
        notification.setIsRead(false);
        notificationRepository.save(notification);

        // send WebSocket notification immediately
        try {
            webSocketMessagingService.sendMessageToUser("system", recipientUserId, new NotificationResponse(notification));
        } catch (Exception ex) {
            log.warn("Failed to send websocket notification to {}: {}", recipientUserId, ex.getMessage());
        }

        // attempt to queue an email notification for durability
        try {
            queueEmailNotification(recipientEmail, title, message);
        } catch (Exception ex) {
            log.warn("Failed to queue email notification for {}: {}", recipientEmail, ex.getMessage());
        }
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
        notificationRepository.markAllAsRead(userId);
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
        return notificationRepository.countUnread(userId);
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

    private void publishRabbitIfAvailable(String rolePrefix, String userId, AppointmentNotificationPayload payload) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.APPOINTMENT_EXCHANGE, rolePrefix + "." + userId, payload);
        } catch (AmqpException ex) {
            log.warn("RabbitMQ not available, websocket delivery still succeeded: {}", ex.getMessage());
        }
    }
}