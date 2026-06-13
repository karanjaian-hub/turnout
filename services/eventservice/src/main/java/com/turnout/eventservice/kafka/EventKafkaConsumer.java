package com.turnout.eventservice.kafka;

import com.turnout.common.enums.RsvpStatus;
import com.turnout.common.kafka.KafkaEvents;
import com.turnout.eventservice.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventKafkaConsumer {

    private final EventService eventService;

    @KafkaListener(topics = KafkaEvents.RSVP_SUBMITTED, groupId = "event-service-group")
    public void handleRsvpSubmitted(KafkaEvents.RsvpSubmittedEvent event) {
        // Only CONFIRMED RSVPs count toward the capacity counter
        if (event.status() != RsvpStatus.CONFIRMED) return;

        log.debug("RSVP confirmed for event {}, incrementing count", event.eventId());
        eventService.incrementRsvpCount(event.eventId());
    }
}
