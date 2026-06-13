package com.turnout.guestservice.service;

import com.turnout.guestservice.dto.GuestResponse;
import com.turnout.guestservice.entity.Guest;
import org.springframework.stereotype.Component;

@Component
public class GuestMapper {

    public GuestResponse toResponse(Guest guest) {
        return new GuestResponse(
                guest.getId(),
                guest.getEventId(),
                guest.getFullName(),
                guest.getEmail(),
                guest.getRsvpStatus(),
                guest.isTokenUsed(),
                guest.getRsvpDate(),
                guest.getCreatedAt()
        );
    }
}
