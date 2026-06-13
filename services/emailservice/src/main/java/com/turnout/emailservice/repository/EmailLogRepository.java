package com.turnout.emailservice.repository;

import com.turnout.emailservice.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findByRecipientEmailOrderByAttemptedAtDesc(String email);

    List<EmailLog> findByStatus(String status);
}
