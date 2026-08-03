package com.turnout.paymentservice.controller;

import com.turnout.common.dto.ApiResponse;
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

    // ------------------------------------------------------------------ //
    //  M-Pesa                                                              //
    // ------------------------------------------------------------------ //

    @PostMapping("/upgrade/mpesa")
    public ResponseEntity<ApiResponse<StkPushResponse>> initiateMpesaUpgrade(
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

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("STK Push initiated successfully", response));
    }

    // WHY no wrapper: Safaricom expects a plain 200 OK — not our envelope format
    @PostMapping("/mpesa/callback")
    public ResponseEntity<Void> mpesaCallback(@RequestBody Map<String, Object> callbackBody) {
        mpesaService.processMpesaCallback(callbackBody);
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------ //
    //  Stripe                                                              //
    // ------------------------------------------------------------------ //

    @PostMapping("/upgrade/stripe")
    public ResponseEntity<ApiResponse<StripeSessionResponse>> initiateStripeUpgrade(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody StripeUpgradeRequest request) {

        StripeSessionResponse response = stripeService.createCheckoutSession(
                userId,
                request.planId(),
                request.successUrl(),
                request.cancelUrl()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Stripe checkout session created", response));
    }

    // WHY no wrapper: Stripe expects a plain 200 OK — not our envelope format
    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String stripeSignature) {

        stripeService.processWebhook(payload, stripeSignature);
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------ //
    //  Subscription & Plans                                                //
    // ------------------------------------------------------------------ //

    @GetMapping("/subscription/me")
    public ResponseEntity<ApiResponse<UserSubscription>> getMySubscription(
            @RequestHeader("X-User-Id") UUID userId) {

        UserSubscription subscription = subscriptionRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserSubscription", userId));

        return ResponseEntity.ok(
                ApiResponse.success("Subscription retrieved", subscription));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlan>>> getActivePlans() {
        List<SubscriptionPlan> plans = planRepo.findAllByActiveTrue();
        return ResponseEntity.ok(
                ApiResponse.success("Plans retrieved", plans));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<PaymentTransaction>>> getMyTransactions(
            @RequestHeader("X-User-Id") UUID userId) {

        return ResponseEntity.ok(
                ApiResponse.success("Transactions retrieved", transactionRepo.findByUserId(userId)));
    }

    // ------------------------------------------------------------------ //
    //  Enterprise Upgrade Requests                                         //
    // ------------------------------------------------------------------ //

    @PostMapping("/upgrade/enterprise")
    public ResponseEntity<ApiResponse<UpgradeRequest>> requestEnterpriseUpgrade(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody EnterpriseUpgradeRequest request) {

        boolean alreadyPending = upgradeRequestRepo.existsByUserIdAndStatus(
                userId, UpgradeRequestStatus.PENDING);

        if (alreadyPending) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("You already have a pending upgrade request"));
        }

        UpgradeRequest upgradeRequest = UpgradeRequest.builder()
                .userId(userId)
                .requestedPlan(request.requestedPlan())
                .status(UpgradeRequestStatus.PENDING)
                .adminNotes(request.notes())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Upgrade request submitted", upgradeRequestRepo.save(upgradeRequest)));
    }

    @PatchMapping("/upgrade/approve/{id}")
    public ResponseEntity<ApiResponse<UpgradeRequest>> approveUpgradeRequest(
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

        return ResponseEntity.ok(
                ApiResponse.success("Upgrade request approved", upgradeRequestRepo.save(request)));
    }

    @PatchMapping("/upgrade/reject/{id}")
    public ResponseEntity<ApiResponse<UpgradeRequest>> rejectUpgradeRequest(
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

        return ResponseEntity.ok(
                ApiResponse.success("Upgrade request rejected", upgradeRequestRepo.save(request)));
    }

    // ------------------------------------------------------------------ //
    //  Tier Check — internal, consumed by event-service                   //
    // ------------------------------------------------------------------ //

    @GetMapping("/tier-check/{userId}")
    public ResponseEntity<ApiResponse<TierLimitsResponse>> getTierLimits(
            @PathVariable UUID userId) {

        return ResponseEntity.ok(
                ApiResponse.success("Tier limits retrieved", tierCheckService.getTierLimits(userId)));
    }

    // ------------------------------------------------------------------ //
    //  Gap 2 — All transactions (ADMIN only, paginated, filterable)       //
    // ------------------------------------------------------------------ //

    @GetMapping("/transactions/all")
    public ResponseEntity<ApiResponse<Page<PaymentTransactionResponse>>> getAllTransactions(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) PaymentProvider provider,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        requireAdminRole(role);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentTransaction> rawPage = transactionRepo.findAll(pageable);

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

        Page<PaymentTransactionResponse> resultPage =
                new PageImpl<>(enriched, pageable, rawPage.getTotalElements());

        return ResponseEntity.ok(
                ApiResponse.success("Transactions retrieved", resultPage));
    }

    // ------------------------------------------------------------------ //
    //  Gap 3 — Pending enterprise upgrade requests (ADMIN only)           //
    // ------------------------------------------------------------------ //

    @GetMapping("/upgrade/requests")
    public ResponseEntity<ApiResponse<Page<UpgradeRequestResponse>>> getUpgradeRequests(
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
                ApiResponse.success("Upgrade requests retrieved",
                        new PageImpl<>(enriched.subList(start, end), pageable, enriched.size())));
    }

    // ------------------------------------------------------------------ //
    //  Gap 4 — Edit tier limits (SUPER_ADMIN only)                        //
    // ------------------------------------------------------------------ //

    @PutMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlan>> updatePlan(
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

        kafkaTemplate.send("turnout.plan.updated", updated.getId().toString(), Map.of(
                "planId",            updated.getId().toString(),
                "planName",          updated.getPlanName(),
                "maxEvents",         updated.getMaxEvents(),
                "maxGuestsPerEvent", updated.getMaxGuestsPerEvent(),
                "timestamp",         LocalDateTime.now().toString()
        ));

        log.info("Plan {} updated by SUPER_ADMIN", updated.getPlanName());
        return ResponseEntity.ok(
                ApiResponse.success("Plan updated successfully", updated));
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

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
