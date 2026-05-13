package com.paypal.wallet_service.service;

import com.paypal.wallet_service.dto.*;
import com.paypal.wallet_service.entity.*;
import com.paypal.wallet_service.exception.*;
import com.paypal.wallet_service.repository.*;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log =
            LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final WalletHoldRepository walletHoldRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(
            WalletRepository walletRepository,
            WalletHoldRepository walletHoldRepository,
            TransactionRepository transactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.walletHoldRepository = walletHoldRepository;
        this.transactionRepository = transactionRepository;
    }

    // CREATE WALLET

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {

        Wallet wallet = new Wallet();

        wallet.setUserId(request.getUserId());
        wallet.setCurrency(request.getCurrency());

        // Initial balances
        wallet.setBalance(0L);
        wallet.setAvailableBalance(0L);

        Wallet savedWallet = walletRepository.save(wallet);

        log.info(
                "Wallet created successfully: userId={}, walletId={}",
                savedWallet.getUserId(),
                savedWallet.getId()
        );

        return mapToResponse(savedWallet);
    }

    // CREDIT MONEY

    @Transactional
    public WalletResponse credit(CreditRequest request) {

        // Validate amount
        validateAmount(request.getAmount());

        // Fetch wallet
        Wallet wallet = getWalletForUpdate(
                request.getUserId(),
                request.getCurrency()
        );

        // Add money
        wallet.setBalance(
                wallet.getBalance() + request.getAmount()
        );

        wallet.setAvailableBalance(
                wallet.getAvailableBalance() + request.getAmount()
        );

        // Save updated wallet
        walletRepository.save(wallet);

        // Save transaction history
        transactionRepository.save(
                new Transaction(
                        wallet,
                        TransactionType.CREDIT,
                        request.getAmount(),
                        TransactionStatus.SUCCESS,
                        UUID.randomUUID().toString()
                )
        );

        log.info(
                "CREDIT success: userId={}, amount={}, balance={}",
                request.getUserId(),
                request.getAmount(),
                wallet.getBalance()
        );

        return mapToResponse(wallet);
    }

    // DEBIT MONEY

    @Transactional
    public WalletResponse debit(DebitRequest request) {

        // Validate amount
        validateAmount(request.getAmount());

        // Fetch wallet
        Wallet wallet = getWalletForUpdate(
                request.getUserId(),
                request.getCurrency()
        );

        // Check available balance
        if (wallet.getAvailableBalance() < request.getAmount()) {

            // Save failed transaction
            transactionRepository.save(
                    new Transaction(
                            wallet,
                            TransactionType.DEBIT,
                            request.getAmount(),
                            TransactionStatus.FAILED,
                            UUID.randomUUID().toString()
                    )
            );

            log.warn(
                    "DEBIT failed: insufficient funds userId={}, amount={}",
                    request.getUserId(),
                    request.getAmount()
            );

            throw new InsufficientFundsException(
                    "Not enough balance"
            );
        }

        // Deduct money
        wallet.setBalance(
                wallet.getBalance() - request.getAmount()
        );

        wallet.setAvailableBalance(
                wallet.getAvailableBalance() - request.getAmount()
        );

        // Save updated wallet
        walletRepository.save(wallet);

        // Save successful transaction
        transactionRepository.save(
                new Transaction(
                        wallet,
                        TransactionType.DEBIT,
                        request.getAmount(),
                        TransactionStatus.SUCCESS,
                        UUID.randomUUID().toString()
                )
        );

        log.info(
                "DEBIT success: userId={}, amount={}, balance={}",
                request.getUserId(),
                request.getAmount(),
                wallet.getBalance()
        );

        return mapToResponse(wallet);
    }

    // GET WALLET DETAILS

    public WalletResponse getWallet(
            Long userId,
            String currency
    ) {

        Wallet wallet = walletRepository
                .findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() ->
                        new NotFoundException("Wallet not found")
                );

        return mapToResponse(wallet);
    }

    // PLACE HOLD

    @Transactional
    public HoldResponse placeHold(HoldRequest request) {

        // Validate amount
        validateAmount(request.getAmount());

        // Fetch wallet
        Wallet wallet = getWalletForUpdate(
                request.getUserId(),
                request.getCurrency()
        );

        // Check available balance
        if (wallet.getAvailableBalance() < request.getAmount()) {

            throw new InsufficientFundsException(
                    "Not enough balance to hold"
            );
        }

        // Reduce only available balance
        wallet.setAvailableBalance(
                wallet.getAvailableBalance() - request.getAmount()
        );

        // Create hold
        WalletHold hold = new WalletHold();

        hold.setWallet(wallet);
        hold.setAmount(request.getAmount());

        hold.setHoldReference(generateHoldReference());

        hold.setStatus(HoldStatus.ACTIVE);

        // Save wallet
        walletRepository.save(wallet);

        // Save hold
        walletHoldRepository.save(hold);

        log.info(
                "HOLD placed: userId={}, amount={}, holdRef={}",
                request.getUserId(),
                request.getAmount(),
                hold.getHoldReference()
        );

        return new HoldResponse(
                hold.getHoldReference(),
                hold.getAmount(),
                hold.getStatus().name()
        );
    }

    // CAPTURE HOLD

    @Transactional
    public WalletResponse captureHold(CaptureRequest request) {

        // Find hold
        WalletHold hold = walletHoldRepository
                .findByHoldReference(request.getHoldReference())
                .orElseThrow(() ->
                        new NotFoundException("Hold not found")
                );

        // Hold must be ACTIVE
        if (hold.getStatus() != HoldStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Hold is not active"
            );
        }

        Wallet wallet = hold.getWallet();

        // Deduct actual balance
        wallet.setBalance(
                wallet.getBalance() - hold.getAmount()
        );

        // Mark hold as captured
        hold.setStatus(HoldStatus.CAPTURED);

        // Save changes
        walletRepository.save(wallet);
        walletHoldRepository.save(hold);

        log.info(
                "HOLD captured: holdRef={}, amount={}",
                request.getHoldReference(),
                hold.getAmount()
        );

        return mapToResponse(wallet);
    }

    // RELEASE HOLD

    @Transactional
    public HoldResponse releaseHold(String holdReference) {

        // Find hold
        WalletHold hold = walletHoldRepository
                .findByHoldReference(holdReference)
                .orElseThrow(() ->
                        new NotFoundException("Hold not found")
                );

        // Hold must be ACTIVE
        if (hold.getStatus() != HoldStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Hold is not active"
            );
        }

        Wallet wallet = hold.getWallet();

        // Restore available balance
        wallet.setAvailableBalance(
                wallet.getAvailableBalance() + hold.getAmount()
        );

        // Mark hold as released
        hold.setStatus(HoldStatus.RELEASED);

        // Save changes
        walletRepository.save(wallet);
        walletHoldRepository.save(hold);

        log.info(
                "HOLD released: holdRef={}, amount={}",
                holdReference,
                hold.getAmount()
        );

        return new HoldResponse(
                hold.getHoldReference(),
                hold.getAmount(),
                hold.getStatus().name()
        );
    }

    // HELPER METHODS

    private Wallet getWalletForUpdate(
            Long userId,
            String currency
    ) {

        return walletRepository
                .findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() ->
                        new NotFoundException("Wallet not found")
                );
    }

    private void validateAmount(Long amount) {

        if (amount == null || amount <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than 0"
            );
        }
    }

    private String generateHoldReference() {

        return "HOLD-" + UUID.randomUUID();
    }

    private WalletResponse mapToResponse(Wallet wallet) {

        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getCurrency(),
                wallet.getBalance(),
                wallet.getAvailableBalance()
        );
    }
}