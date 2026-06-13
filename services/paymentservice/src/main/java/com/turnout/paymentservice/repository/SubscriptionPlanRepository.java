package com.turnout.paymentservice.repository;

import com.turnout.paymentservice.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    Optional<SubscriptionPlan> findByPlanNameIgnoreCase(String planName);

    List<SubscriptionPlan> findAllByActiveTrue();
}
