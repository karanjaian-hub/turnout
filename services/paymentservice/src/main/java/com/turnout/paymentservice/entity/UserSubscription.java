package com.turnout.paymentservice.entity;

import com.turnout.common.entity.BaseEntity;
import com.turnout.paymentservice.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tracks which plan each user is currently on.
 * One active subscription per user at a time — enforced in the service layer.
 */
@Entity
@Table(schema = "payment", name = "user_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate renewalDate;
}
