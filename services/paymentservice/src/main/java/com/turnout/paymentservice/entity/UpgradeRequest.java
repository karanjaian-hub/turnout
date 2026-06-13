package com.turnout.paymentservice.entity;

import com.turnout.common.entity.BaseEntity;
import com.turnout.paymentservice.enums.UpgradeRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Used ONLY for ENTERPRISE tier upgrades.
 * Enterprise pricing is custom — a user requests it, an admin approves or rejects.
 * FREE -> PRO upgrades skip this entirely and go through Stripe / M-Pesa directly.
 */
@Entity
@Table(schema = "payment", name = "upgrade_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpgradeRequest extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String requestedPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UpgradeRequestStatus status = UpgradeRequestStatus.PENDING;

    // Admin fills this in when approving or rejecting
    @Column(columnDefinition = "TEXT")
    private String adminNotes;
}
