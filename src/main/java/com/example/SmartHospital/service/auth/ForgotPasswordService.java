package com.example.SmartHospital.service.auth;

import java.io.IOException;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.config.RabbitMQConfig;
import com.example.SmartHospital.dtos.AuthDtos.Request.AuthRequests.OtpEmailMessage;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService {
    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String EMAIL_KEY_PREFIX = "email:";
    private static final String RESET_EMAIL_KEY_PREFIX = "reset-email:";
    private static final String RESET_TOKEN_KEY_PREFIX = "reset-token:";
    private static final String LOGIN_OTP_KEY_PREFIX = "login-otp:";
    private static final String LOGIN_EMAIL_KEY_PREFIX = "login-email:";
    private static final String LOGIN_TOKEN_KEY_PREFIX = "login-token:";

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.mail.username}")
    private String emailHost;
    

    @Async
    public CompletableFuture<String> sendOtp(String email) { // CompletableFuture<String> help to return token asynchronously
        return sendOtp(email, "password-reset");
    }

    @Async
    public CompletableFuture<String> sendTwoFactorOtp(String email) {
        return sendOtp(email, "login");
    }

    private CompletableFuture<String> sendOtp(String email, String purpose) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found"));
        
        String otp = generateOTP();
        String token = UUID.randomUUID().toString();

        OtpEmailMessage otpEmailMessage = new OtpEmailMessage(token, email, user.getFullName(), otp, purpose);
        // Queue email task so OTP mail is durable and can be retried if mail provider is unavailable.
        rabbitTemplate.convertAndSend(RabbitMQConfig.OTP_EXCHANGE, RabbitMQConfig.OTP_ROUTING_KEY, otpEmailMessage);
        return CompletableFuture.completedFuture(token);
    }

    @RabbitListener(queues = RabbitMQConfig.OTP_QUEUE)
    public void consumeOtpEmail(OtpEmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "utf-8");

            helper.setFrom(emailHost, "SmartHospital Support");
            helper.setTo(message.getEmail());
            helper.setSubject("[SmartHospital] Your " + ("login".equalsIgnoreCase(message.getPurpose()) ? "login verification" : "password reset") + " OTP");
            String htmlContent = getHTMLContent(message.getOtp(), message.getFullName());
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

            // Start expiration only after email is successfully sent to avoid early OTP timeout.
            String otpKeyPrefix = "login".equalsIgnoreCase(message.getPurpose()) ? LOGIN_OTP_KEY_PREFIX : OTP_KEY_PREFIX;
            String emailKeyPrefix = "login".equalsIgnoreCase(message.getPurpose()) ? LOGIN_EMAIL_KEY_PREFIX : EMAIL_KEY_PREFIX;
            redisTemplate.opsForValue().set(otpKeyPrefix + message.getToken(), message.getOtp(), Duration.ofMinutes(5));
            redisTemplate.opsForValue().set(emailKeyPrefix + message.getToken(), message.getEmail(), Duration.ofMinutes(5));
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to send OTP email from queue", e);
        }
    }

    public CompletableFuture<String> verifyOtp(String token, String otp) {
        try {
            String value = redisTemplate.opsForValue().get(OTP_KEY_PREFIX + token);

            if (value == null) {
                throw new IllegalArgumentException("OTP expired or invalid");
            }

            String savedOtp = value;

            if (!savedOtp.equals(otp)) {
                throw new IllegalArgumentException("Invalid OTP");
            }
            
            String email = redisTemplate.opsForValue().get(EMAIL_KEY_PREFIX + token);
            String resetToken = UUID.randomUUID().toString();
            userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found"));

            // Start the timer for password reset token
            redisTemplate.opsForValue().set(
                RESET_EMAIL_KEY_PREFIX + resetToken,
                email,
                Duration.ofMinutes(15)
            );
            redisTemplate.opsForValue().set(
                RESET_TOKEN_KEY_PREFIX + resetToken,
                resetToken,
                Duration.ofMinutes(15)
            );
            redisTemplate.delete(EMAIL_KEY_PREFIX + token);
            redisTemplate.delete(OTP_KEY_PREFIX + token);
            return CompletableFuture.completedFuture(resetToken); // Return a password reset token
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify OTP", e);
        }
    }

    public CompletableFuture<String> verifyTwoFactorOtp(String token, String otp) {
        try {
            String value = redisTemplate.opsForValue().get(LOGIN_OTP_KEY_PREFIX + token);
            if (value == null) {
                throw new IllegalArgumentException("OTP expired or invalid");
            }

            if (!value.equals(otp)) {
                throw new IllegalArgumentException("Invalid OTP");
            }

            String email = redisTemplate.opsForValue().get(LOGIN_EMAIL_KEY_PREFIX + token);
            if (email == null) {
                throw new IllegalArgumentException("User with email not found");
            }

            String loginToken = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(
                LOGIN_TOKEN_KEY_PREFIX + loginToken,
                email,
                Duration.ofMinutes(15)
            );
            redisTemplate.delete(LOGIN_EMAIL_KEY_PREFIX + token);
            redisTemplate.delete(LOGIN_OTP_KEY_PREFIX + token);
            return CompletableFuture.completedFuture(loginToken);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify OTP", e);
        }
    }

    public String getLoginEmailForToken(String token) {
        return redisTemplate.opsForValue().get(LOGIN_TOKEN_KEY_PREFIX + token);
    }

    public void consumeLoginToken(String token) {
        redisTemplate.delete(LOGIN_TOKEN_KEY_PREFIX + token);
    }

    public void resetPassword(String token, String newPassword) {
        try {
            // Validate password strength
            if (newPassword.length() < 8 ||
                !newPassword.matches(".*[A-Z].*") ||
                !newPassword.matches(".*[a-z].*") ||
                !newPassword.matches(".*\\d.*") ||
                !newPassword.matches(".*[!@#$%^&*()].*")) {
                throw new IllegalArgumentException("Password is not strong enough");
            }
            String email = redisTemplate.opsForValue().get(RESET_EMAIL_KEY_PREFIX + token);
            if (email == null) {
                throw new IllegalArgumentException("Invalid or expired password reset token");
            }
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found"));
            user.setHashedPassword(passwordEncoder.encode(newPassword)); 
            userRepository.save(user);
                redisTemplate.delete(RESET_EMAIL_KEY_PREFIX + token); // Invalidate the reset token after use
                redisTemplate.delete(RESET_TOKEN_KEY_PREFIX + token);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset password", e);
        }
        
    }
    
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); 
        return String.valueOf(otp);
    }

    private String getHTMLContent(String otp,  String fullName) {
        ClassPathResource resource = new ClassPathResource("mailTemplate/otpMail.html");
        try (var inputStream = resource.getInputStream()) {
            String content = new String(inputStream.readAllBytes());
            return content.replace("{{OTP}}", otp)
                          .replace("{{name}}", fullName);
        } catch(Exception e){
            throw new RuntimeException("Fail to find HTMLTemplate", e);
        }
    }


}
