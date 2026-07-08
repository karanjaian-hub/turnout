package com.turnout.guestservice.service;

import com.turnout.common.enums.RsvpStatus;
import com.turnout.guestservice.dto.EventGuestStatsResponse;
import com.turnout.guestservice.dto.GuestResponse;
import com.turnout.guestservice.dto.UpdateGuestRequest;
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

    public GuestResponse getGuest(UUID guestId) {
        return guestRepository.findById(guestId)
                .map(guestMapper::toResponse)
                .orElseThrow(() -> new GuestNotFoundException(guestId));
    }

    public EventGuestStatsResponse getEventStats(UUID eventId) {
        long confirmed  = guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.CONFIRMED);
        long declined   = guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.DECLINED);
        long pending    = guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.PENDING);
        long maybe      = guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.MAYBE);
        long waitlisted = guestRepository.countByEventIdAndRsvpStatus(eventId, RsvpStatus.WAITLISTED);

        return new EventGuestStatsResponse(
                eventId,
                confirmed + declined + pending + maybe + waitlisted,
                confirmed, declined, pending, maybe, waitlisted
        );
    }

    @Transactional
    public GuestResponse updateGuest(UUID guestId, UpdateGuestRequest request) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new GuestNotFoundException(guestId));

        guest.setFullName(request.fullName());
        guest.setEmail(request.email().toLowerCase());

        return guestMapper.toResponse(guestRepository.save(guest));
    }

    @Transactional
    public void deleteGuest(UUID guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new GuestNotFoundException(guestId));

        if (guest.isTokenUsed()) {
            throw new IllegalStateException(
                "Cannot delete guest who has already submitted an RSVP"
            );
        }

        guestRepository.delete(guest);
        log.info("Deleted guest {} from event {}", guestId, guest.getEventId());
    }
}
