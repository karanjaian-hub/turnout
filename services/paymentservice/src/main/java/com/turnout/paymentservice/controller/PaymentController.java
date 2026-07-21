package com.turnout.paymentservice.controller;

import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.common.exception.UnauthorizedAccessException;
import com.turnout.paymentservice.dto.*;
import com.turnout.paymentservice.entity.SubscriptionPlan;
import com.turnout.paymentservice.entity.UpgradeRequest;
import com.turnout.paymentservice.entity.UserSubscription;
import com.turnout.paymentservice.entity.PaymentTransaction;
import com.turnout.paymentservice.enums.PaymentProvider;
import com.turnout.paymentservice.enums.PaymentStatus;
import com.turnout.paymentservice.enums.UpgradeRequestStatus;
import com.turnout.paymentservice.repository.SubscriptionPlanRepository;
import com.turnout.paymentservice.repository.PaymentTransactionRepository;
import com.turnout.paymentservice.repository.UpgradeRequestRepository;
import com.turnout.paymentservice.repository.UserSubscriptionRepository;
import com.turnout.paymentservice.service.MpesaService;
import com.turnout.paymentservice.service.StripeService;
import com.turnout.paymentservice.service.TierCheckService;
import com.turnout.paymentservice.service.UserLookupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final MpesaService                 mpesaService;
    private final StripeService                stripeService;
    private final TierCheckService             tierCheckService;
    private final UserLookupService            userLookupService;
    private final SubscriptionPlanRepository   planRepo;
    private final UserSubscriptionRepository   subscriptionRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final UpgradeRequestRepository     upgradeRequestRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;

//  M-Pesa //
    @PostMapping("/upgrade/mpesa")
    public ResponseEntity<StkPushResponse> initiateMpesaUpgrade(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody MpesaUpgradeRequest request) {

        SubscriptionPlan plan = planRepo.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", request.planId()));

        StkPushResponse response = mpesaService.initiateStk(
                request.phoneNumber(),
                plan.getMonthlyPriceKes(),
                request.accountRef(),
                userId,
                plan.getId()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/mpesa/callback")
    public ResponseEntity<Void> mpesaCallback(@RequestBody Map<String, Object> callbackBody) {
        mpesaService.processMpesaCallback(callbackBody);
        return ResponseEntity.ok().build();
    }

//  Stripe  //
    @PostMapping("/upgrade/stripe")
    public ResponseEntity<StripeSessionResponse> initiateStripeUpgrade(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody StripeUpgradeRequest request) {

        StripeSessionResponse response = stripeService.createCheckoutSession(
                userId,
                request.planId(),
                request.successUrl(),
                request.cancelUrl()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * WHY @RequestBody String: we need the raw payload to verify HMAC-SHA256.
     * If Spring deserializes it to Map first, byte ordering may differ and
     * signature verification will fail.
     */
    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String stripeSignature) {

        stripeService.processWebhook(payload, stripeSignature);
        return ResponseEntity.ok().build();
    }

// Subscription & Plans //
    @GetMapping("/subscription/me")
    public ResponseEntity<UserSubscription> getMySubscription(
            @RequestHeader("X-User-Id") UUID userId) {

        UserSubscription subscription = subscriptionRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserSubscription", userId));

        return ResponseEntity.ok(subscription);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getActivePlans() {
        return ResponseEntity.ok(planRepo.findAllByActiveTrue());
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getMyTransactions(
            @RequestHeader("X-User-Id") UUID userId) {

        return ResponseEntity.ok(transactionRepo.findByUserId(userId));
    }

//  Enterprise Upgrade Requests                                         //
    @PostMapping("/upgrade/enterprise")
    public ResponseEntity<UpgradeRequest> requestEnterpriseUpgrade(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody EnterpriseUpgradeRequest request) {

        boolean alreadyPending = upgradeRequestRepo.existsByUserIdAndStatus(
                userId, UpgradeRequestStatus.PENDING);

        if (alreadyPending) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        UpgradeRequest upgradeRequest = UpgradeRequest.builder()
                .userId(userId)
                .requestedPlan(request.requestedPlan())
                .status(UpgradeRequestStatus.PENDING)
                .adminNotes(request.notes())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(upgradeRequestRepo.save(upgradeRequest));
    }

    @PatchMapping("/upgrade/approve/{id}")
    public ResponseEntity<UpgradeRequest> approveUpgradeRequest(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id,
            @RequestBody(required = false) AdminReviewRequest reviewRequest) {

        requireAdminRole(role);

        UpgradeRequest request = upgradeRequestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UpgradeRequest", id));

        request.setStatus(UpgradeRequestStatus.APPROVED);
        if (reviewRequest != null && reviewRequest.adminNotes() != null) {
            request.setAdminNotes(reviewRequest.adminNotes());
        }

        return ResponseEntity.ok(upgradeRequestRepo.save(request));
    }

    @PatchMapping("/upgrade/reject/{id}")
    public ResponseEntity<UpgradeRequest> rejectUpgradeRequest(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id,
            @RequestBody(required = false) AdminReviewRequest reviewRequest) {

        requireAdminRole(role);

        UpgradeRequest request = upgradeRequestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UpgradeRequest", id));

        request.setStatus(UpgradeRequestStatus.REJECTED);
        if (reviewRequest != null && reviewRequest.adminNotes() != null) {
            request.setAdminNotes(reviewRequest.adminNotes());
        }

        return ResponseEntity.ok(upgradeRequestRepo.save(request));
    }

//  Tier Check — internal, consumed by event-service //
    @GetMapping("/tier-check/{userId}")
    public ResponseEntity<TierLimitsResponse> getTierLimits(@PathVariable UUID userId) {
        return ResponseEntity.ok(tierCheckService.getTierLimits(userId));
    }

//  Gap 2 — All transactions (ADMIN only, paginated, filterable) //
    /**
     * Returns all payment transactions across all users — admin Payments page.
     * Optional filters: provider, status. Sorted by createdAt desc.
     * Each record is enriched with username/email via a WebClient call to auth-service.
     */
    @GetMapping("/transactions/all")
    public ResponseEntity<Page<PaymentTransactionResponse>> getAllTransactions(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) PaymentProvider provider,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        requireAdminRole(role);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentTransaction> rawPage = transactionRepo.findAll(pageable);

// Filter in-memory after fetch — avoids a custom JPQL query for optional combos
        List<PaymentTransaction> filtered = rawPage.getContent().stream()
                .filter(tx -> provider == null || tx.getProvider() == provider)
                .filter(tx -> status   == null || tx.getStatus()   == status)
                .collect(Collectors.toList());

        List<PaymentTransactionResponse> enriched = filtered.stream()
                .map(tx -> {
                    UserLookupResponse user = userLookupService.lookup(tx.getUserId());
                    return new PaymentTransactionResponse(
                            tx.getId(),
                            tx.getUserId(),
                            user.username(),
                            user.email(),
                            tx.getPlanId(),
                            tx.getProvider(),
                            tx.getAmount(),
                            tx.getCurrency(),
                            tx.getStatus(),
                            tx.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new PageImpl<>(enriched, pageable, rawPage.getTotalElements()));
    }

// Gap 3 — Pending enterprise upgrade requests (ADMIN only) //
   @GetMapping("/upgrade/requests")
    public ResponseEntity<Page<UpgradeRequestResponse>> getUpgradeRequests(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "PENDING") UpgradeRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        requireAdminRole(role);

        List<UpgradeRequest> requests = upgradeRequestRepo.findByStatus(status);

        List<UpgradeRequestResponse> enriched = requests.stream()
                .map(req -> {
                    UserLookupResponse user = userLookupService.lookup(req.getUserId());
                    return new UpgradeRequestResponse(
                            req.getId(),
                            req.getUserId(),
                            user.username(),
                            user.email(),
                            req.getRequestedPlan(),
                            req.getStatus(),
                            req.getAdminNotes(),
                            req.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), enriched.size());

        return ResponseEntity.ok(
                new PageImpl<>(enriched.subList(start, end), pageable, enriched.size()));
    }


//  Edit tier limits (SUPER_ADMIN only) //
// Allows SUPER_ADMIN to update plan limits from the Settings page.
    /**
     *
     * Only non-null fields in the request body are applied — partial update.
     * Publishes 'turnout.plan.updated' so event-service and guest-service
     * can invalidate any cached tier-limit lookups.
     */
    @PutMapping("/plans/{id}")
    public ResponseEntity<SubscriptionPlan> updatePlan(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id,
            @RequestBody UpdatePlanRequest request) {

        requireSuperAdminRole(role);

        SubscriptionPlan plan = planRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", id));

        if (request.maxEvents()         != null) plan.setMaxEvents(request.maxEvents());
        if (request.maxGuestsPerEvent() != null) plan.setMaxGuestsPerEvent(request.maxGuestsPerEvent());
        if (request.monthlyPriceKes()   != null) plan.setMonthlyPriceKes(request.monthlyPriceKes());
        if (request.monthlyPriceUsd()   != null) plan.setMonthlyPriceUsd(request.monthlyPriceUsd());
        if (request.active()            != null) plan.setActive(request.active());

        SubscriptionPlan updated = planRepo.save(plan);

// Notify other services that cached limits need refreshing
        kafkaTemplate.send("turnout.plan.updated", updated.getId().toString(), Map.of(
                "planId",           updated.getId().toString(),
                "planName",         updated.getPlanName(),
                "maxEvents",        updated.getMaxEvents(),
                "maxGuestsPerEvent",updated.getMaxGuestsPerEvent(),
                "timestamp",        LocalDateTime.now().toString()
        ));

        log.info("Plan {} updated by SUPER_ADMIN", updated.getPlanName());
        return ResponseEntity.ok(updated);
    }

//  Private helpers //
    private void requireAdminRole(String role) {
        if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            throw new UnauthorizedAccessException("Admin access required");
        }
    }

    private void requireSuperAdminRole(String role) {
        if (!"SUPER_ADMIN".equals(role)) {
            throw new UnauthorizedAccessException("Super admin access required");
        }
    }
}
