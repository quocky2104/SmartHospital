package com.example.SmartHospital.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "app.notifications.rabbit", name = "enabled", havingValue = "true")
public class RabbitMQConfig {
    // RabbitMQ configuration for appointment notifications and OTP emails
    public static final String APPOINTMENT_EXCHANGE = "appointment.notifications.exchange";
    public static final String APPOINTMENT_QUEUE = "appointment.notifications.queue";
    public static final String APPOINTMENT_ROUTING_KEY_PATTERN = "doctor.*";

    public static final String OTP_EXCHANGE = "auth.otp.exchange";
    public static final String OTP_QUEUE = "auth.otp.email.queue";
    public static final String OTP_ROUTING_KEY = "auth.otp.send";
 
    // Define RabbitMQ beans for appointment notifications
    // Topic exchange routes messages to queues using a binding key pattern with dot-separated words 
    // ex: doctor.123 is doctor with ID 123
    // *: matches exactly one word, so doctor.* matches doctor.123 but not doctor.123.extra
    // #: matches zero or more words, so doctor.# matches doctor.123 and doctor.123.extra
    @Bean
    public TopicExchange appointmentNotificationExchange() {
        return new TopicExchange(APPOINTMENT_EXCHANGE, true, false);
    }

    // Define the queue for appointment notifications and bind it to the exchange
    // Durable queues ensure that messages are not lost in case of RabbitMQ restarts
    @Bean
    public Queue appointmentNotificationQueue() {
        return new Queue(APPOINTMENT_QUEUE, true);
    }

    // Bind the appointment notification queue to the topic exchange with the routing key pattern
    // ex: messages sent to the exchange with routing key doctor.123 
    // will be routed to the appointmentNotificationQueue
    @Bean
    public Binding appointmentNotificationBinding(
        Queue appointmentNotificationQueue,
        TopicExchange appointmentNotificationExchange
    ) {
        return BindingBuilder.bind(appointmentNotificationQueue)
            .to(appointmentNotificationExchange)
            .with(APPOINTMENT_ROUTING_KEY_PATTERN);
    }

    // Define RabbitMQ beans for OTP email notifications
    @Bean
    public TopicExchange otpExchange() {
        return new TopicExchange(OTP_EXCHANGE, true, false);
    }

    // Define the queue for OTP email notifications and bind it to the exchange
    @Bean
    public Queue otpQueue() {
        return new Queue(OTP_QUEUE, true);
    }

    // Bind the OTP queue to the exchange with the routing key
    @Bean
    public Binding otpBinding(Queue otpQueue, TopicExchange otpExchange) {
        return BindingBuilder.bind(otpQueue)
            .to(otpExchange)
            .with(OTP_ROUTING_KEY);
    }
}