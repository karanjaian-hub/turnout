package com.turnout.paymentservice.entity;

import com.turnout.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Defines the available subscription tiers: FREE, PRO, ENTERPRISE.
 * Seeded once at DB startup — not created by users.
 * maxEvents / maxGuestsPerEvent = -1 means unlimited.
 */
@Entity
@Table(schema = "payment", name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String planName;

    @Column(nullable = false)
    private int maxEvents;           // -1 = unlimited

    @Column(nullable = false)
    private int maxGuestsPerEvent;   // -1 = unlimited

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPriceKes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPriceUsd;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
