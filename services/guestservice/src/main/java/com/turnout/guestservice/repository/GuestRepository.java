package com.turnout.guestservice.repository;

import com.turnout.common.enums.RsvpStatus;
import com.turnout.guestservice.entity.Guest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRepository extends JpaRepository<Guest, UUID> {

    // Paginated list — for the API endpoint that lists guests for an event
    Page<Guest> findByEventId(UUID eventId, Pageable pageable);

    // Full list — for CSV export (no pagination, we need everything)
    List<Guest> findByEventId(UUID eventId);

    // RSVP service uses this when a guest clicks their link
    Optional<Guest> findByToken(String token);

    // Used during import to fetch an existing guest record if needed
    Optional<Guest> findByEventIdAndEmail(UUID eventId, String email);

    // Faster duplicate check during import — just yes/no, no object returned
    boolean existsByEventIdAndEmail(UUID eventId, String email);

    // Stats endpoint — count per status (e.g. how many CONFIRMED for this event)
    long countByEventIdAndRsvpStatus(UUID eventId, RsvpStatus rsvpStatus);

    // Email service uses this to fetch all PENDING guests to send invitations
    List<Guest> findByEventIdAndRsvpStatus(UUID eventId, RsvpStatus rsvpStatus);
}
