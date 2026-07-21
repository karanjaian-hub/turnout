package com.turnout.emailservice.repository;

import com.turnout.emailservice.entity.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Page<EmailLog> findByEventIdOrderByAttemptedAtDesc(UUID eventId, Pageable pageable);

    List<EmailLog> findByGuestIdOrderByAttemptedAtDesc(String guestId);

    List<EmailLog> findByRecipientEmailOrderByAttemptedAtDesc(String email);

    List<EmailLog> findByStatus(String status);

    // Used by sendInvitationEmail to locate the QUEUED row EmailController pre-created
    Optional<EmailLog> findFirstByGuestIdAndEventIdAndEventTypeAndStatusOrderByAttemptedAtDesc(
            String guestId, UUID eventId, String eventType, String status);

    @Query("SELECT e.status, COUNT(e) FROM EmailLog e WHERE e.eventId = :eventId GROUP BY e.status")
    List<Object[]> countByStatusForEvent(@Param("eventId") UUID eventId);
}
