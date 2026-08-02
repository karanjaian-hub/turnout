package com.turnout.paymentservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnout.common.exception.ResourceNotFoundException;
import com.turnout.paymentservice.config.MpesaProperties;
import com.turnout.paymentservice.dto.StkPushResponse;
import com.turnout.paymentservice.entity.PaymentTransaction;
import com.turnout.paymentservice.enums.PaymentProvider;
import com.turnout.paymentservice.enums.PaymentStatus;
import com.turnout.paymentservice.repository.PaymentTransactionRepository;
import com.turnout.paymentservice.repository.SubscriptionPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MpesaService {

    private static final String MPESA_TOKEN_KEY  = "mpesa:token";
    private static final String TRANSACTION_TYPE = "CustomerPayBillOnline";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // Safaricom's Timestamp/Password must reflect Kenyan local time (EAT, UTC+3).
    // Containers default to UTC unless configured otherwise, so this must be
    // explicit rather than relying on the JVM's default zone.
    private static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");

    private final MpesaProperties               mpesaProps;
    private final StringRedisTemplate           redisTemplate;
    private final PaymentTransactionRepository  transactionRepo;
    private final SubscriptionPlanRepository    planRepo;
    private final SubscriptionService           subscriptionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper                  objectMapper;
    private final WebClient                     mpesaWebClient;

    // Manual constructor — @Qualifier on a field is ignored by @RequiredArgsConstructor.
    // It must be on the constructor parameter to work correctly.
    public MpesaService(
            MpesaProperties mpesaProps,
            StringRedisTemplate redisTemplate,
            PaymentTransactionRepository transactionRepo,
            SubscriptionPlanRepository planRepo,
            SubscriptionService subscriptionService,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            @Qualifier("mpesaWebClient") WebClient mpesaWebClient) {
        this.mpesaProps          = mpesaProps;
        this.redisTemplate       = redisTemplate;
        this.transactionRepo     = transactionRepo;
        this.planRepo            = planRepo;
        this.subscriptionService = subscriptionService;
        this.kafkaTemplate       = kafkaTemplate;
        this.objectMapper        = objectMapper;
        this.mpesaWebClient      = mpesaWebClient;
    }

    // ── Access Token ──────────────────────────────────────────────────────────

    /**
     * Fetches a Daraja OAuth token, caching it in Redis for 3500s.
     * WHY 3500s not 3600s: Safaricom tokens expire in 1 hour. We shave
     * 100 seconds off to avoid using a token that expires mid-request.
     */
    public String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(MPESA_TOKEN_KEY);
        if (cached != null) {
            return cached;
        }

        String credentials = Base64.getEncoder().encodeToString(
                (mpesaProps.getConsumerKey() + ":" + mpesaProps.getConsumerSecret())
                        .getBytes(StandardCharsets.UTF_8)
        );

        Map<?, ?> response;
        try {
            response = mpesaWebClient.get()
                    .uri("/oauth/v1/generate?grant_type=client_credentials")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Safaricom OAuth token request failed [{}]: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }

        String token = (String) response.get("access_token");
        redisTemplate.opsForValue().set(MPESA_TOKEN_KEY, token, Duration.ofSeconds(3500));
        return token;
    }

    // ── STK Push ──────────────────────────────────────────────────────────────

    /**
     * Sends an STK Push (payment prompt) to the user's phone.
     * Safaricom calls our callback URL once the user pays or cancels.
     */
    public StkPushResponse initiateStk(String phoneNumber,
                                       BigDecimal amount,
                                       String accountRef,
                                       UUID userId,
                                       UUID planId) {

        // Must be Nairobi local time — see NAIROBI constant above.
        String timestamp = LocalDateTime.now(NAIROBI).format(TIMESTAMP_FMT);

        // Password = Base64(shortcode + passkey + timestamp) — Safaricom's spec
        String rawPassword = mpesaProps.getShortcode() + mpesaProps.getPasskey() + timestamp;
        String password    = Base64.getEncoder()
                .encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = new HashMap<>();
        body.put("BusinessShortCode", mpesaProps.getShortcode());
        body.put("Password",          password);
        body.put("Timestamp",         timestamp);
        body.put("TransactionType",   TRANSACTION_TYPE);
        body.put("Amount",            amount.intValue());
        body.put("PartyA",            phoneNumber);
        body.put("PartyB",            mpesaProps.getShortcode());
        body.put("PhoneNumber",       phoneNumber);
        body.put("CallBackURL",       mpesaProps.getCallbackUrl());
        body.put("AccountReference",  accountRef);
        body.put("TransactionDesc",   "Turnout subscription payment");

        Map<?, ?> response;
        try {
            response = mpesaWebClient.post()
                    .uri("/mpesa/stkpush/v1/processrequest")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Safaricom STK push failed [{}]: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(
                    "M-Pesa payment failed: " + e.getResponseBodyAsString());
        }

        String checkoutRequestId = (String) response.get("CheckoutRequestID");

        PaymentTransaction transaction = PaymentTransaction.builder()
                .userId(userId)
                .planId(planId)
                .provider(PaymentProvider.MPESA)
                .amount(amount)
                .currency("KES")
                .status(PaymentStatus.PENDING)
                .providerReference(checkoutRequestId)
                .build();
        transactionRepo.save(transaction);

        log.info("STK Push initiated for user={} checkoutRequestId={}", userId, checkoutRequestId);
        return new StkPushResponse(checkoutRequestId, "STK Push sent. Check your phone.");
    }

    // ── Callback ──────────────────────────────────────────────────────────────

    /**
     * Safaricom POSTs this to /api/payments/mpesa/callback after the user
     * completes or cancels the payment prompt on their phone.
     * ResultCode == 0 means success; anything else means failure.
     */
    public void processMpesaCallback(Map<String, Object> callbackBody) {
        try {
            JsonNode root        = objectMapper.valueToTree(callbackBody);
            JsonNode stkCallback = root.path("Body").path("stkCallback");

            String checkoutRequestId = stkCallback.path("CheckoutRequestID").asText();
            int    resultCode        = stkCallback.path("ResultCode").asInt();

            PaymentTransaction transaction = transactionRepo
                    .findByProviderReference(checkoutRequestId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "PaymentTransaction", checkoutRequestId));

            if (resultCode == 0) {
                transaction.setStatus(PaymentStatus.COMPLETED);
                transactionRepo.save(transaction);
                subscriptionService.upgradeSubscription(
                        transaction.getUserId(), transaction.getPlanId());
                publishSubscriptionUpgraded(transaction);
                log.info("M-Pesa payment SUCCESS for user={}", transaction.getUserId());
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
                transaction.setMetadata(stkCallback.toString());
                transactionRepo.save(transaction);
                publishPaymentFailed(transaction);
                log.warn("M-Pesa payment FAILED for user={} resultCode={}",
                        transaction.getUserId(), resultCode);
            }
        } catch (Exception e) {
            log.error("Error processing M-Pesa callback", e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void publishSubscriptionUpgraded(PaymentTransaction tx) {
        Map<String, Object> event = Map.of(
                "userId",    tx.getUserId().toString(),
                "planId",    tx.getPlanId().toString(),
                "provider",  tx.getProvider().name(),
                "timestamp", LocalDateTime.now().toString()
        );
        kafkaTemplate.send("subscription.upgraded", tx.getUserId().toString(), event);
    }

    private void publishPaymentFailed(PaymentTransaction tx) {
        Map<String, Object> event = Map.of(
                "userId",    tx.getUserId().toString(),
                "planId",    tx.getPlanId().toString(),
                "provider",  tx.getProvider().name(),
                "timestamp", LocalDateTime.now().toString()
        );
        kafkaTemplate.send("payment.failed", tx.getUserId().toString(), event);
    }
}