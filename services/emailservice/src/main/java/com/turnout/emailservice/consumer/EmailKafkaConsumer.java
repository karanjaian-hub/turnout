package com.turnout.emailservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnout.emailservice.dto.PasswordResetEvent;
import com.turnout.emailservice.dto.UserRegisteredEvent;
import com.turnout.emailservice.service.EmailDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailKafkaConsumer {

    private final EmailDispatchService emailDispatchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.registered", groupId = "emailservice-group")
    public void handleUserRegistered(String message) {
        log.debug("Received user.registered event: {}", message);
        try {
            UserRegisteredEvent event = objectMapper.readValue(message, UserRegisteredEvent.class);
            emailDispatchService.sendVerificationEmail(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getVerificationToken()
            );
        } catch (Exception e) {
            // Rethrow so KafkaConsumerConfig's DefaultErrorHandler can apply retry + backoff.
            // If we swallow the exception here, Kafka thinks the message was processed
            // successfully and will never retry it.
            log.error("Failed to process user.registered event: {}", e.getMessage());
            throw new RuntimeException("Failed to process user.registered event", e);
        }
    }

    @KafkaListener(topics = "user.password-reset", groupId = "emailservice-group")
    public void handlePasswordReset(String message) {
        log.debug("Received user.password-reset event: {}", message);
        try {
            PasswordResetEvent event = objectMapper.readValue(message, PasswordResetEvent.class);
            emailDispatchService.sendPasswordResetEmail(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getResetToken()
            );
        } catch (Exception e) {
            log.error("Failed to process user.password-reset event: {}", e.getMessage());
            throw new RuntimeException("Failed to process user.password-reset event", e);
        }
    }
}
