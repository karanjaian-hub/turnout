package com.turnout.paymentservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.paymentservice.config.StripeProperties;
import com.turnout.paymentservice.dto.StripeSessionResponse;
import com.turnout.paymentservice.entity.PaymentTransaction;
import com.turnout.paymentservice.entity.SubscriptionPlan;
import com.turnout.paymentservice.enums.PaymentProvider;
import com.turnout.paymentservice.enums.PaymentStatus;
import com.turnout.paymentservice.repository.PaymentTransactionRepository;
import com.turnout.paymentservice.repository.SubscriptionPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class StripeService {

    private final StripeProperties              stripeProps;
    private final PaymentTransactionRepository  transactionRepo;
    private final SubscriptionPlanRepository    planRepo;
    private final SubscriptionService           subscriptionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper                  objectMapper;
    private final WebClient                     stripeWebClient;

    // Manual constructor — @Qualifier on a field is ignored by @RequiredArgsConstructor
    public StripeService(
            StripeProperties stripeProps,
            PaymentTransactionRepository transactionRepo,
            SubscriptionPlanRepository planRepo,
            SubscriptionService subscriptionService,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            @Qualifier("stripeWebClient") WebClient stripeWebClient) {
        this.stripeProps         = stripeProps;
        this.transactionRepo     = transactionRepo;
        this.planRepo            = planRepo;
        this.subscriptionService = subscriptionService;
        this.kafkaTemplate       = kafkaTemplate;
        this.objectMapper        = objectMapper;
        this.stripeWebClient     = stripeWebClient;
    }

    // ── Checkout Session ──────────────────────────────────────────────────────

    public StripeSessionResponse createCheckoutSession(UUID userId,
                                                       UUID planId,
                                                       String successUrl,
                                                       String cancelUrl) {

        SubscriptionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", planId));

        long amountCents = plan.getMonthlyPriceUsd()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("mode",                                   "subscription");
        body.add("success_url",                            successUrl);
        body.add("cancel_url",                             cancelUrl);
        body.add("line_items[0][price_data][currency]",    "usd");
        body.add("line_items[0][price_data][unit_amount]", String.valueOf(amountCents));
        body.add("line_items[0][price_data][product_data][name]",
                 "Turnout " + plan.getPlanName() + " Plan");
        body.add("line_items[0][price_data][recurring][interval]", "month");
        body.add("line_items[0][quantity]",                "1");
        body.add("metadata[userId]",                       userId.toString());
        body.add("metadata[planId]",                       planId.toString());

        JsonNode response = stripeWebClient.post()
                .uri("/v1/checkout/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + stripeProps.getSecretKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(body))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        String sessionId   = response.path("id").asText();
        String checkoutUrl = response.path("url").asText();

        PaymentTransaction transaction = PaymentTransaction.builder()
                .userId(userId)
                .planId(planId)
                .provider(PaymentProvider.STRIPE)
                .amount(plan.getMonthlyPriceUsd())
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .providerReference(sessionId)
                .build();
        transactionRepo.save(transaction);

        log.info("Stripe checkout session created for user={} sessionId={}", userId, sessionId);
        return new StripeSessionResponse(sessionId, checkoutUrl);
    }

    // ── Webhook ───────────────────────────────────────────────────────────────

    public void processWebhook(String payload, String stripeSignature) {
        if (!isValidStripeSignature(payload, stripeSignature)) {
            log.warn("Stripe webhook signature verification failed");
            return;
        }

        try {
            JsonNode event     = objectMapper.readTree(payload);
            String   eventType = event.path("type").asText();

            switch (eventType) {
                case "checkout.session.completed" -> handleCheckoutCompleted(event);
                case "invoice.payment_failed"     -> handlePaymentFailed(event);
                default -> log.debug("Unhandled Stripe event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void handleCheckoutCompleted(JsonNode event) {
        JsonNode session   = event.path("data").path("object");
        String   sessionId = session.path("id").asText();
        UUID     userId    = UUID.fromString(session.path("metadata").path("userId").asText());
        UUID     planId    = UUID.fromString(session.path("metadata").path("planId").asText());

        transactionRepo.findByProviderReference(sessionId).ifPresent(tx -> {
            tx.setStatus(PaymentStatus.COMPLETED);
            transactionRepo.save(tx);
        });

        subscriptionService.upgradeSubscription(userId, planId);
        publishSubscriptionUpgraded(userId, planId, PaymentProvider.STRIPE);
        log.info("Stripe checkout completed for user={}", userId);
    }

    private void handlePaymentFailed(JsonNode event) {
        JsonNode invoice   = event.path("data").path("object");
        String   sessionId = invoice.path("subscription").asText();

        transactionRepo.findByProviderReference(sessionId).ifPresent(tx -> {
            tx.setStatus(PaymentStatus.FAILED);
            transactionRepo.save(tx);
            publishPaymentFailed(tx.getUserId(), tx.getPlanId());
            log.warn("Stripe payment failed for user={}", tx.getUserId());
        });
    }

    private boolean isValidStripeSignature(String payload, String stripeSignature) {
        try {
            String timestamp = extractSignatureComponent(stripeSignature, "t");
            String expected  = extractSignatureComponent(stripeSignature, "v1");
            String signed    = timestamp + "." + payload;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    stripeProps.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            String computed = HexFormat.of().formatHex(
                    mac.doFinal(signed.getBytes(StandardCharsets.UTF_8))
            );
            return computed.equals(expected);
        } catch (Exception e) {
            log.error("Stripe signature verification error", e);
            return false;
        }
    }

    private String extractSignatureComponent(String header, String key) {
        for (String part : header.split(",")) {
            String[] kv = part.strip().split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return "";
    }

    private void publishSubscriptionUpgraded(UUID userId, UUID planId, PaymentProvider provider) {
        kafkaTemplate.send("subscription.upgraded", userId.toString(), Map.of(
                "userId",    userId.toString(),
                "planId",    planId.toString(),
                "provider",  provider.name(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    private void publishPaymentFailed(UUID userId, UUID planId) {
        kafkaTemplate.send("payment.failed", userId.toString(), Map.of(
                "userId",    userId.toString(),
                "planId",    planId.toString(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
