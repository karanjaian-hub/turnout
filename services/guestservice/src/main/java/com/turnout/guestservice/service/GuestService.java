package com.turnout.guestservice.service;

import com.turnout.common.enums.RsvpStatus;
import com.turnout.guestservice.dto.EventGuestStatsResponse;
import com.turnout.guestservice.dto.GuestResponse;
import com.turnout.guestservice.entity.Guest;
import com.turnout.guestservice.exception.GuestNotFoundException;
import com.turnout.guestservice.repository.GuestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final GuestMapper guestMapper;

    public Page<GuestResponse> getGuestsByEvent(UUID eventId, Pageable pageable) {
        return guestRepository.findByEventId(eventId, pageable)
                .map(guestMapper::toResponse);
    }

    public EventGuestStatsResponse getEventStats(UUID eventId) {
        long total       = guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.PENDING)
                         + guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.CONFIRMED)
                         + guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.DECLINED)
                         + guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.MAYBE)
                         + guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.WAITLISTED);

        return new EventGuestStatsResponse(
                eventId,
                total,
                guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.CONFIRMED),
                guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.DECLINED),
                guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.PENDING),
                guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.MAYBE),
                guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.WAITLISTED)
        );
    }

    @Transactional
    public void deleteGuest(UUID guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new GuestNotFoundException(guestId));

        // Only allow deletion if guest hasn't responded yet —
        // deleting a CONFIRMED guest silently corrupts the event's capacity count
        if (guest.isTokenUsed()) {
            throw new IllegalStateException(
                "Cannot delete guest who has already submitted an RSVP"
            );
        }

        guestRepository.delete(guest);
        log.info("Deleted guest {} from event {}", guestId, guest.getEventId());
    }
}
