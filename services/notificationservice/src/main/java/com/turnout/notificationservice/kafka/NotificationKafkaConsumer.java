package com.turnout.notificationservice.kafka;

import com.turnout.common.kafka.KafkaEvents.*;
import com.turnout.notificationservice.dto.EmailProgressMessage;
import com.turnout.notificationservice.dto.RsvpUpdateMessage;
import com.turnout.notificationservice.model.DashboardStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${turnout.stats.cache-ttl-seconds}")
    private long statsCacheTtlSeconds;

    private static final String KEY_STATS        = "dashboard:stats:%s";
    private static final String KEY_EMAIL        = "email:progress:%s";
    private static final String KEY_RECENT_RSVP  = "recent-rsvps:%s";

    private static final String DEST_RSVP_UPDATES   = "/topic/rsvp-updates/%s";
    private static final String DEST_EMAIL_PROGRESS  = "/topic/email-progress/%s";
    private static final String DEST_ADMIN_ALERTS    = "/topic/admin-alerts";

    @KafkaListener(topics = com.turnout.common.kafka.KafkaEvents.RSVP_SUBMITTED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleRsvpSubmitted(RsvpSubmittedEvent event) {
        UUID eventId = event.eventId();
        log.info("RSVP submitted for event {}, status {}", eventId, event.status());

        String statsKey = KEY_STATS.formatted(eventId);

        Map<Object, Object> raw = redisTemplate.opsForHash().entries(statsKey);
        DashboardStats stats = DashboardStats.fromMap(raw);

        // RsvpStatus is an enum — use its name() for the switch in DashboardStats
        stats.increment(event.status().name());

        redisTemplate.opsForHash().putAll(statsKey, stats.toMap());
        redisTemplate.expire(statsKey, statsCacheTtlSeconds, TimeUnit.SECONDS);

        RsvpUpdateMessage message = stats.toRsvpUpdateMessage(eventId);
        messagingTemplate.convertAndSend(DEST_RSVP_UPDATES.formatted(eventId), message);

        String recentKey = KEY_RECENT_RSVP.formatted(eventId);
        redisTemplate.opsForList().leftPush(recentKey, event);
        redisTemplate.opsForList().trim(recentKey, 0, 19);

        // Platform-wide feed — maintained separately for the no-eventId dashboard endpoint
        redisTemplate.opsForList().leftPush("recent-rsvps:platform", event);
        redisTemplate.opsForList().trim("recent-rsvps:platform", 0, 49);
    }

    @KafkaListener(topics = com.turnout.common.kafka.KafkaEvents.EMAIL_STATUS,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleEmailStatus(EmailStatusEvent event) {
        log.info("Email status update: log {} status {}", event.emailLogId(), event.status());

        // EmailStatusEvent tracks per-email status, not aggregate progress.
        // We broadcast the status change directly — aggregate progress comes via EmailProgressEvent.
        messagingTemplate.convertAndSend(DEST_ADMIN_ALERTS,
                Map.of("type", "EMAIL_STATUS",
                       "emailLogId", event.emailLogId(),
                       "status", event.status().name(),
                       "timestamp", LocalDateTime.now().toString()));
    }

    @KafkaListener(topics = com.turnout.common.kafka.KafkaEvents.EMAIL_PROGRESS,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handleEmailProgress(EmailProgressEvent event) {
        UUID eventId = event.eventId();
        log.info("Email progress for event {}: {}/{}", eventId, event.sent(), event.total());

        String progressKey = KEY_EMAIL.formatted(eventId);

        redisTemplate.opsForHash().put(progressKey, "totalEmails", String.valueOf(event.total()));
        redisTemplate.opsForHash().put(progressKey, "sentCount",   String.valueOf(event.sent()));
        redisTemplate.opsForHash().put(progressKey, "failedCount", "0");
        redisTemplate.expire(progressKey, 1, TimeUnit.HOURS);

        int progressPercentage = event.total() == 0 ? 0
                : (int) Math.min(100, (event.sent() * 100L) / event.total());

        EmailProgressMessage message = new EmailProgressMessage(
                eventId,
                event.total(),
                event.sent(),
                0L,
                progressPercentage,
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend(DEST_EMAIL_PROGRESS.formatted(eventId), message);
    }

    @KafkaListener(topics = com.turnout.common.kafka.KafkaEvents.PAYMENT_FAILED,
                   groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("Payment failed for user {}: {}", event.userId(), event.reason());
        messagingTemplate.convertAndSend(DEST_ADMIN_ALERTS,
                Map.of("type", "PAYMENT_FAILED",
                       "userId", event.userId().toString(),
                       "reason", event.reason(),
                       "timestamp", LocalDateTime.now().toString()));
    }
}
