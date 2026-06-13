package com.turnout.paymentservice.entity;

import com.turnout.paymentservice.enums.PaymentProvider;
import com.turnout.paymentservice.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit record of every payment attempt.
 * WHY no BaseEntity: this is append-only — we never update a transaction record.
 * An updatedAt column on a payment would be misleading and legally problematic.
 */
@Entity
@Table(schema = "payment", name = "payment_transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentProvider provider;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    private String currency;            // "KES" or "USD"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // M-Pesa CheckoutRequestID or Stripe PaymentIntent ID
    @Column(length = 200)
    private String providerReference;

    // Raw JSON from M-Pesa / Stripe callback — kept as-is for audit trail
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
