package com.turnout.eventservice.entity;

import com.turnout.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;
import com.turnout.common.enums.EventStatus;

@Entity
@Table(schema = "events", name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Future
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @NotBlank
    @Column(nullable = false)
    private String location;

    @Positive
    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity;

    // Incremented by Kafka consumer on each confirmed RSVP — not set directly by API callers
    @Column(name = "current_rsvp_count", nullable = false)
    private int currentRsvpCount = 0;

    // UUID of the user who created this event — logical FK only (no cross-schema constraint)
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;
}
