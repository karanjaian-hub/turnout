package com.turnout.eventservice.repository;

import com.turnout.eventservice.entity.Event;
import com.turnout.common.enums.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByCreatedBy(UUID createdBy);

    List<Event> findByStatus(EventStatus status);

    List<Event> findByCreatedByAndStatus(UUID createdBy, EventStatus status);

    // Pessimistic write lock — used during status transitions so two concurrent
    // PATCH /status calls on the same event can't both proceed simultaneously
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(UUID id);

    long countByCreatedBy(UUID createdBy);
}
