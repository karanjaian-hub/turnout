package com.turnout.eventservice.repository;

import com.turnout.common.enums.EventStatus;
import com.turnout.eventservice.entity.Event;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventSpecification {

    private EventSpecification() {}

    // Each non-null param adds one AND condition — null params are simply skipped.
    // This avoids the Postgres "could not determine data type of parameter $N" error
    // that occurs when passing typed nulls via JPQL.
    public static Specification<Event> withFilters(
            UUID organizerId,
            EventStatus status,
            LocalDateTime dateFrom,
            LocalDateTime dateTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (organizerId != null) {
                predicates.add(cb.equal(root.get("createdBy"), organizerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
