package com.turnout.guestservice.entity;

import com.turnout.common.entity.BaseEntity;
import com.turnout.common.enums.RsvpStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    schema = "guests",
    name = "guests",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_guest_event_email",
        columnNames = {"event_id", "email"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class Guest extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "rsvp_status", nullable = false)
    private RsvpStatus rsvpStatus = RsvpStatus.PENDING;

    // The JWT token embedded in the guest's RSVP link
    @Column(unique = true, nullable = false)
    private String token;

    // Flips to true after the guest submits — prevents double-submission
    @Column(name = "token_used", nullable = false)
    private boolean tokenUsed = false;

    // Null until the guest actually responds
    @Column(name = "rsvp_date")
    private LocalDateTime rsvpDate;

    public Guest(UUID eventId, String fullName, String email, String token) {
        this.eventId = eventId;
        this.fullName = fullName;
        this.email = email;
        this.token = token;
    }
}
