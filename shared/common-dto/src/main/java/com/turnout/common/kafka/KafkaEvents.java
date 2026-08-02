package com.turnout.common.kafka;

import com.turnout.common.enums.EmailStatus;
import com.turnout.common.enums.RsvpStatus;

import java.util.List;
import java.util.UUID;

/**
 * Single source of truth for all Kafka topic names and the event payloads
 * that travel on them. Using Java records keeps these immutable and zero-boilerplate.
 */
public final class KafkaEvents {

    private KafkaEvents() {}

// Topic constants
    public static final String GUEST_IMPORTED        = "guest.imported";
    public static final String EMAIL_SEND            = "email.send";
    public static final String EMAIL_STATUS          = "email.status";
    public static final String RSVP_SUBMITTED        = "rsvp.submitted";
    public static final String DASHBOARD_UPDATE      = "dashboard.update";
    public static final String EMAIL_PROGRESS        = "email.progress";
    public static final String SUBSCRIPTION_UPGRADED = "subscription.upgraded";
    public static final String PAYMENT_FAILED        = "payment.failed";
    public static final String AI_INSIGHT_READY      = "ai.insight.ready";

// Event records

    public record GuestImportedEvent(
            UUID eventId,
            UUID organizerId,
            int totalGuests,
            int successCount,
            int failureCount
    ) {}

    public record EmailSendEvent(
            UUID emailLogId,
            UUID eventId,
            String recipientEmail,
            String recipientName,
            String subject,
            String htmlBody,
            String textBody
    ) {}

    public record EmailStatusEvent(
            UUID emailLogId,
            EmailStatus status,
            String providerMessageId,
            String failureReason
    ) {}

    public record RsvpSubmittedEvent(
            UUID rsvpId,
            UUID eventId,
            UUID guestId,
            String guestEmail,
            String guestName,
            RsvpStatus status
    ) {}

    public record DashboardUpdateEvent(
            UUID eventId,
            UUID organizerId,
            String metricType,
            long value
    ) {}

    public record EmailProgressEvent(
            UUID eventId,
            UUID organizerId,
            int sent,
            int total
    ) {}

    public record SubscriptionUpgradedEvent(
            UUID userId,
            String fromPlan,
            String toPlan,
            String transactionRef
    ) {}

    public record PaymentFailedEvent(
            UUID userId,
            String reason,
            String transactionRef
    ) {}

    public record AiInsightReadyEvent(
            UUID eventId,
            UUID organizerId,
            String insightType,
            String payload
    ) {}
}
