package com.turnout.paymentservice.service;

import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.common.exception.TierLimitExceededException;
import com.turnout.paymentservice.dto.TierLimitsResponse;
import com.turnout.paymentservice.entity.SubscriptionPlan;
import com.turnout.paymentservice.entity.UserSubscription;
import com.turnout.paymentservice.enums.SubscriptionStatus;
import com.turnout.paymentservice.repository.SubscriptionPlanRepository;
import com.turnout.paymentservice.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TierCheckService {

    private final UserSubscriptionRepository subscriptionRepo;
    private final SubscriptionPlanRepository planRepo;

    @Qualifier("eventServiceWebClient")
    private final WebClient eventServiceWebClient;

    /**
     * Fetches the user's active plan.
     * Falls back to FREE if no subscription exists — new users haven't paid yet
     * but must still be able to use the app.
     */
    public SubscriptionPlan getUserPlan(UUID userId) {
        return subscriptionRepo
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(sub -> planRepo.findById(sub.getPlanId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "SubscriptionPlan", sub.getPlanId())))
                .orElseGet(() -> planRepo.findByPlanNameIgnoreCase("FREE")
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "SubscriptionPlan", "FREE")));
    }

    /**
     * Called by event-service before creating a new event.
     * Throws TierLimitExceededException so the controller returns 402.
     */
    public void checkEventLimit(UUID userId) {
        SubscriptionPlan plan = getUserPlan(userId);

        if (plan.getMaxEvents() == -1) {
            return; // unlimited
        }

        int currentCount = fetchCurrentEventCount(userId);

        if (currentCount >= plan.getMaxEvents()) {
            throw new TierLimitExceededException("create more events", plan.getPlanName());
        }
    }

    /**
     * Called by guest-service before a bulk import or manual guest add.
     */
    public void checkGuestLimit(UUID userId, int requestedCount) {
        SubscriptionPlan plan = getUserPlan(userId);

        if (plan.getMaxGuestsPerEvent() == -1) {
            return; // unlimited
        }

        if (requestedCount > plan.getMaxGuestsPerEvent()) {
            throw new TierLimitExceededException("add more guests", plan.getPlanName());
        }
    }

    /**
     * Called by GET /api/payments/tier-check/{userId}.
     * event-service uses this to enforce limits without duplicating subscription logic.
     */
    public TierLimitsResponse getTierLimits(UUID userId) {
        SubscriptionPlan plan         = getUserPlan(userId);
        int              currentCount = fetchCurrentEventCount(userId);

        return new TierLimitsResponse(
                plan.getPlanName(),
                plan.getMaxEvents(),
                plan.getMaxGuestsPerEvent(),
                currentCount
        );
    }

    /**
     * WHY WebClient not a direct DB query: payment-service must not own the events table.
     * Each service owns its own data. Returns 0 on error so a network blip never
     * blocks a user from creating events.
     */
    private int fetchCurrentEventCount(UUID userId) {
        try {
            return eventServiceWebClient.get()
                    .uri("/api/events/count?userId=" + userId)
                    .retrieve()
                    .bodyToMono(Integer.class)
                    .block();
        } catch (Exception e) {
            log.warn("Could not fetch event count for user={} — defaulting to 0. Error: {}",
                    userId, e.getMessage());
            return 0;
        }
    }
}
