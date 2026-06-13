package com.turnout.paymentservice.service;

import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.paymentservice.entity.SubscriptionPlan;
import com.turnout.paymentservice.entity.UserSubscription;
import com.turnout.paymentservice.enums.SubscriptionStatus;
import com.turnout.paymentservice.repository.SubscriptionPlanRepository;
import com.turnout.paymentservice.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserSubscriptionRepository subscriptionRepo;
    private final SubscriptionPlanRepository planRepo;

    /**
     * Creates or replaces the user's active subscription.
     * Called after a successful payment from either M-Pesa or Stripe.
     * WHY @Transactional: we expire the old plan and create the new one
     * atomically — we never want a user in limbo with no active plan.
     */
    @Transactional
    public UserSubscription upgradeSubscription(UUID userId, UUID planId) {
        SubscriptionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", planId));

        // Expire any existing active subscription
        subscriptionRepo.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRepo.save(existing);
                });

        UserSubscription newSubscription = UserSubscription.builder()
                .userId(userId)
                .planId(planId)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .renewalDate(LocalDate.now().plusMonths(1))
                .build();

        UserSubscription saved = subscriptionRepo.save(newSubscription);
        log.info("User {} upgraded to plan {}", userId, plan.getPlanName());
        return saved;
    }
}
