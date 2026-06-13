package com.turnout.emailservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnout.emailservice.config.BrevoProperties;
import com.turnout.emailservice.dto.BrevoEmailRequest;
import com.turnout.emailservice.entity.EmailLog;
import com.turnout.emailservice.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDispatchService {

    private final WebClient webClient;
    private final BrevoProperties brevoProperties;
    private final EmailLogRepository emailLogRepository;
    private final ObjectMapper objectMapper;

    // Entry point for the consumer. Builds the Brevo request, fires it, and logs the result.
    public void sendVerificationEmail(String recipientEmail, String recipientName, String token) {
        String verifyUrl = "https://app.turnout.com/verify?token=" + token;

        String html = """
                <h2>Welcome to Turnout, %s!</h2>
                <p>Click the link below to verify your email address:</p>
                <a href="%s">Verify Email</a>
                <p>This link expires in 24 hours.</p>
                """.formatted(recipientName, verifyUrl);

        BrevoEmailRequest request = buildRequest(recipientEmail, recipientName,
                "Verify your Turnout account", html);

        dispatch(request, recipientEmail, recipientName, "USER_REGISTERED");
    }

    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String token) {
        String resetUrl = "https://app.turnout.com/reset-password?token=" + token;

        String html = """
                <h2>Password Reset Request</h2>
                <p>Hi %s,</p>
                <p>Click the link below to reset your password:</p>
                <a href="%s">Reset Password</a>
                <p>If you didn't request this, you can safely ignore this email.</p>
                <p>This link expires in 1 hour.</p>
                """.formatted(recipientName, resetUrl);

        BrevoEmailRequest request = buildRequest(recipientEmail, recipientName,
                "Reset your Turnout password", html);

        dispatch(request, recipientEmail, recipientName, "PASSWORD_RESET");
    }

    // Shared method: sends the request to Brevo and persists the result either way.
    private void dispatch(BrevoEmailRequest request, String recipientEmail,
                          String recipientName, String eventType) {
        EmailLog emailLog = EmailLog.builder()
                .eventType(eventType)
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .subject(request.getSubject())
                .status("PENDING")
                .attemptedAt(LocalDateTime.now())
                .build();

        try {
            // Brevo returns a JSON body with a messageId on success
            String responseBody = webClient.post()
                    .uri("/smtp/email")
                    .header("api-key", brevoProperties.getApi().getKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Blocking is intentional here — Kafka consumers run on their own
                              // thread pool, so we don't risk blocking the main event loop

            emailLog.setStatus("SUCCESS");
            emailLog.setProviderResponse(responseBody);
            emailLog.setDeliveredAt(LocalDateTime.now());

            log.info("Email sent successfully to {} for event {}", recipientEmail, eventType);

        } catch (Exception e) {
            emailLog.setStatus("FAILED");
            emailLog.setProviderResponse(e.getMessage());

            // We log but don't rethrow — the Kafka consumer handles retry logic.
            // Rethrowing here would cause the same message to be consumed again immediately.
            log.error("Failed to send email to {} for event {}: {}", recipientEmail, eventType, e.getMessage());
        }

        emailLogRepository.save(emailLog);
    }

    private BrevoEmailRequest buildRequest(String recipientEmail, String recipientName,
                                           String subject, String htmlContent) {
        return BrevoEmailRequest.builder()
                .sender(BrevoEmailRequest.Sender.builder()
                        .name(brevoProperties.getSender().getName())
                        .email(brevoProperties.getSender().getEmail())
                        .build())
                .to(List.of(BrevoEmailRequest.Recipient.builder()
                        .email(recipientEmail)
                        .name(recipientName)
                        .build()))
                .subject(subject)
                .htmlContent(htmlContent)
                .build();
    }
}
