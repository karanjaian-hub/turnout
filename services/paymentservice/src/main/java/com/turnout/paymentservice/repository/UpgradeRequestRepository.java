package com.turnout.paymentservice.repository;

import com.turnout.paymentservice.entity.UpgradeRequest;
import com.turnout.paymentservice.enums.UpgradeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UpgradeRequestRepository extends JpaRepository<UpgradeRequest, UUID> {

    List<UpgradeRequest> findByUserId(UUID userId);

    List<UpgradeRequest> findByStatus(UpgradeRequestStatus status);

    // Guard: prevent a user from submitting duplicate pending requests
    boolean existsByUserIdAndStatus(UUID userId, UpgradeRequestStatus status);
}
