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

    Page<Guest> findByEventId(UUID eventId, Pageable pageable);

    List<Guest> findByEventId(UUID eventId);

    Optional<Guest> findByToken(String token);

    Optional<Guest> findByEventIdAndEmail(UUID eventId, String email);

    boolean existsByEventIdAndEmail(UUID eventId, String email);

    long countByEventIdAndRsvpStatus(UUID eventId, RsvpStatus rsvpStatus);

    // Email service uses this to fetch all PENDING guests to send invitations
    List<Guest> findByEventIdAndRsvpStatus(UUID eventId, RsvpStatus rsvpStatus);
}
