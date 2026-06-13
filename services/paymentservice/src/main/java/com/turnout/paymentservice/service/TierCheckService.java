package com.turnout.paymentservice.service;

import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.common.exception.TierLimitExceededException;
import com.turnout.paymentservice.dto.TierLimitsResponse;
import com.turnout.paymentservice.entity.SubscriptionPlan;
import com.turnout.paymentservice.repository.SubscriptionPlanRepository;
import com.turnout.paymentservice.repository.UserSubscriptionRepository;
import com.turnout.paymentservice.enums.SubscriptionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Service
public class TierCheckService {

    private final UserSubscriptionRepository subscriptionRepo;
    private final SubscriptionPlanRepository planRepo;
    private final WebClient                  eventServiceWebClient;

    // Manual constructor — @Qualifier on a field is ignored by @RequiredArgsConstructor
    public TierCheckService(
            UserSubscriptionRepository subscriptionRepo,
            SubscriptionPlanRepository planRepo,
            @Qualifier("eventServiceWebClient") WebClient eventServiceWebClient) {
        this.subscriptionRepo      = subscriptionRepo;
        this.planRepo              = planRepo;
        this.eventServiceWebClient = eventServiceWebClient;
    }

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

    public void checkEventLimit(UUID userId) {
        SubscriptionPlan plan = getUserPlan(userId);
        if (plan.getMaxEvents() == -1) return;

        int currentCount = fetchCurrentEventCount(userId);
        if (currentCount >= plan.getMaxEvents()) {
            throw new TierLimitExceededException("create more events", plan.getPlanName());
        }
    }

    public void checkGuestLimit(UUID userId, int requestedCount) {
        SubscriptionPlan plan = getUserPlan(userId);
        if (plan.getMaxGuestsPerEvent() == -1) return;

        if (requestedCount > plan.getMaxGuestsPerEvent()) {
            throw new TierLimitExceededException("add more guests", plan.getPlanName());
        }
    }

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
