package com.turnout.paymentservice.repository;

import com.turnout.paymentservice.entity.PaymentTransaction;
import com.turnout.paymentservice.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByUserId(UUID userId);

    Page<PaymentTransaction> findByUserId(UUID userId, Pageable pageable);

    // Used by M-Pesa callback to look up the pending transaction by Safaricom's reference ID
    Optional<PaymentTransaction> findByProviderReference(String providerReference);

    List<PaymentTransaction> findByUserIdAndStatus(UUID userId, PaymentStatus status);
}
