package com.paypal.wallet_service.scheduler;

import com.paypal.wallet_service.entity.HoldStatus;
import com.paypal.wallet_service.entity.WalletHold;
import com.paypal.wallet_service.repository.WalletHoldRepository;
import com.paypal.wallet_service.service.WalletService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class HoldExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryScheduler.class);

    private final WalletHoldRepository walletHoldRepository;
    private final WalletService walletService;

    public HoldExpiryScheduler(WalletHoldRepository walletHoldRepository,
                               WalletService walletService) {
        this.walletHoldRepository = walletHoldRepository;
        this.walletService = walletService;
    }

    @Scheduled(fixedDelayString = "${wallet.hold.expiry.scan-rate-ms:60000}")
    @Transactional
    public void expireOldHolds() {

        LocalDateTime now = LocalDateTime.now();

        List<WalletHold> expiredHolds =
                walletHoldRepository.findByStatusAndExpiresAtBefore(HoldStatus.ACTIVE, now);

        if (expiredHolds.isEmpty()) {
            log.debug("No expired holds found at {}", now);
            return;
        }

        log.info("Found {} expired holds", expiredHolds.size());

        for (WalletHold hold : expiredHolds) {
            processHold(hold);
        }
    }

    private void processHold(WalletHold hold) {

        String ref = hold.getHoldReference();

        try {
            walletService.releaseHold(ref);
            log.info("Expired hold released: {}", ref);
        } catch (Exception e) {
            log.error("Failed to release expired hold {}", ref, e);
        }
    }
}