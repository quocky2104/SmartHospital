package com.example.SmartHospital.service.notification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.example.SmartHospital.dtos.NotificationDtos.AppointmentNotificationPayload;
import com.example.SmartHospital.service.messaging.WebSocketMessagingService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationSender implements INotificationSender {
    private final WebSocketMessagingService webSocketMessagingService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    // This method sends appointment notifications via WebSocket
    @Override
    public void sendAppointmentAlert(String receiverId, AppointmentNotificationPayload payload) {
        webSocketMessagingService.sendAppointmentNotification("system", receiverId, payload);
    }

    // This method sends appointment notifications via email
    @Override
    public void sendAppointmentEmail(String toEmail, String receiverName, AppointmentNotificationPayload payload) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || toEmail == null || toEmail.isBlank()) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(toEmail);
            helper.setSubject("[SmartHospital] Appointment " + payload.getEventType());
            String content = String.format(
                "Hello %s,%n%n%s%n%nAppointment ID: %s%nDate Time: %s%n%nSmartHospital",
                receiverName,
                payload.getMessage(),
                payload.getAppointmentId(),
                payload.getAppointmentDateTime()
            );
            helper.setText(content, false);
            mailSender.send(message);
        } catch (MessagingException ex) {
            log.warn("Failed to send appointment email to {}: {}", toEmail, ex.getMessage());
        }
    }
}
