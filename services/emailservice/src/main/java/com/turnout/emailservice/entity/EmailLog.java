package com.turnout.emailservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_logs", schema = "email")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID eventId;

    private String guestId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String recipientEmail;

    private String recipientName;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String providerResponse;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    private LocalDateTime deliveredAt;
}
