package com.turnout.paymentservice.controller;

import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.common.exception.UnauthorizedAccessException;
import com.turnout.paymentservice.dto.*;
import com.turnout.paymentservice.entity.SubscriptionPlan;
import com.turnout.paymentservice.entity.UpgradeRequest;
import com.turnout.paymentservice.entity.UserSubscription;
import com.turnout.paymentservice.enums.UpgradeRequestStatus;
import com.turnout.paymentservice.repository.SubscriptionPlanRepository;
import com.turnout.paymentservice.repository.PaymentTransactionRepository;
import com.turnout.paymentservice.repository.UpgradeRequestRepository;
import com.turnout.paymentservice.repository.UserSubscriptionRepository;
import com.turnout.paymentservice.service.MpesaService;
import com.turnout.paymentservice.service.StripeService;
import com.turnout.paymentservice.service.TierCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final MpesaService              mpesaService;
    private final StripeService             stripeService;
    private final TierCheckService          tierCheckService;
    private final SubscriptionPlanRepository planRepo;
    private final UserSubscriptionRepository subscriptionRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final UpgradeRequestRepository  upgradeRequestRepo;

    // ------------------------------------------------------------------ //
    //  M-Pesa                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Initiates an STK Push to the user's phone.
     * Returns 202 Accepted — the actual payment result arrives asynchronously
     * via the M-Pesa callback endpoint below.
     */
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

    /**
     * Public endpoint — Safaricom POSTs here after the user completes or
     * cancels the STK prompt. No JWT, signature verified inside MpesaService.
     */
    @PostMapping("/mpesa/callback")
    public ResponseEntity<Void> mpesaCallback(@RequestBody Map<String, Object> callbackBody) {
        mpesaService.processMpesaCallback(callbackBody);
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------ //
    //  Stripe                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Creates a Stripe-hosted checkout session and returns the URL.
     * The frontend redirects the user to that URL to complete payment.
     */
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
     * Public endpoint — Stripe POSTs signed events here.
     * WHY @RequestBody String: we need the raw payload bytes to verify
     * the HMAC signature. If we let Spring deserialize it to a Map first,
     * the byte order may change and signature verification will fail.
     */
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

    // ------------------------------------------------------------------ //
    //  Enterprise Upgrade Requests                                         //
    // ------------------------------------------------------------------ //

    /**
     * Users on any tier can request an Enterprise upgrade.
     * Guard: reject if they already have a pending request — no duplicates.
     */
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

    /**
     * ADMIN / SUPER_ADMIN only — approves an enterprise upgrade request.
     * WHY we check X-User-Role here and not via Spring Security:
     * the gateway already validated the JWT and forwarded the role as a header.
     * We trust that header because only the gateway sets it — external clients can't.
     */
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

    // ------------------------------------------------------------------ //
    //  Tier Check — consumed by event-service                             //
    // ------------------------------------------------------------------ //

    /**
     * Called internally by event-service before creating events or adding guests.
     * Not exposed to end users — event-service calls this service-to-service.
     */
    @GetMapping("/tier-check/{userId}")
    public ResponseEntity<TierLimitsResponse> getTierLimits(@PathVariable UUID userId) {
        return ResponseEntity.ok(tierCheckService.getTierLimits(userId));
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    private void requireAdminRole(String role) {
        if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            throw new UnauthorizedAccessException("Admin access required");
        }
    }
}
