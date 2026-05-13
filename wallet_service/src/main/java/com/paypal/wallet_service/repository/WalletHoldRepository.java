package com.paypal.wallet_service.repository;

import com.paypal.wallet_service.entity.HoldStatus;
import com.paypal.wallet_service.entity.WalletHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WalletHoldRepository extends JpaRepository<WalletHold, Long> {

    // Find hold by unique reference
    Optional<WalletHold> findByHoldReference(String holdReference);

    // Fetch all expired ACTIVE holds
    List<WalletHold> findByStatusAndExpiresAtBefore(HoldStatus status, LocalDateTime now);
}