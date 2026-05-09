package com.example.SmartHospital.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
    public static final String APPOINTMENT_ROUTING_KEY_PATTERN_DOCTOR = "doctor.*";
    public static final String APPOINTMENT_ROUTING_KEY_PATTERN_PATIENT = "patient.*";

    public static final String EMAIL_EXCHANGE = "email.notifications.exchange";
    public static final String EMAIL_QUEUE = "email.notifications.queue";
    public static final String EMAIL_ROUTING_KEY_PATTERN = "email.*";

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

    // Bind the appointment notification queue to the topic exchange with the routing key patterns
    // ex: messages sent to the exchange with routing key doctor.123 or patient.123
    // will be routed to the appointmentNotificationQueue
    @Bean
    public Binding appointmentNotificationBindingDoctor(
        Queue appointmentNotificationQueue,
        TopicExchange appointmentNotificationExchange
    ) {
        return BindingBuilder.bind(appointmentNotificationQueue)
            .to(appointmentNotificationExchange)
            .with(APPOINTMENT_ROUTING_KEY_PATTERN_DOCTOR);
    }

    @Bean
    public Binding appointmentNotificationBindingPatient(
        Queue appointmentNotificationQueue,
        TopicExchange appointmentNotificationExchange
    ) {
        return BindingBuilder.bind(appointmentNotificationQueue)
            .to(appointmentNotificationExchange)
            .with(APPOINTMENT_ROUTING_KEY_PATTERN_PATIENT);
    }

    // Define RabbitMQ beans for email notifications
    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailQueue)
            .to(emailExchange)
            .with(EMAIL_ROUTING_KEY_PATTERN);
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

    // Configure RabbitTemplate and MessageConverter for JSON serialization of messages
    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // RabbitTemplate is used to send messages to RabbitMQ, and it needs to be configured with the connection factory and message converter
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }
    
    // Configure the RabbitListenerContainerFactory to use the same message converter for listeners that consume messages from RabbitMQ
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }
}