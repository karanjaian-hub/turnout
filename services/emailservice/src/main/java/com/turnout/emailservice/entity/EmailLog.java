package com.turnout.emailservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The Kafka topic this email request came from (e.g. "user.registered")
    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String recipientEmail;

    private String recipientName;

    // Subject line we sent to Brevo
    @Column(nullable = false)
    private String subject;

    // SUCCESS or FAILED — stored as string so it's readable in the DB without enum mappings
    @Column(nullable = false)
    private String status;

    // Brevo's message ID on success, or the error message on failure
    @Column(columnDefinition = "TEXT")
    private String providerResponse;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    // Populated only on success
    private LocalDateTime deliveredAt;
}
