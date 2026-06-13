package com.turnout.paymentservice.repository;

import com.turnout.paymentservice.entity.UserSubscription;
import com.turnout.paymentservice.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    Optional<UserSubscription> findByUserId(UUID userId);

    Optional<UserSubscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);
}
