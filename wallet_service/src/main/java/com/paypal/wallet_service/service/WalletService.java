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

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final WalletHoldRepository walletHoldRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(
            WalletRepository walletRepository,
            WalletHoldRepository walletHoldRepository,
            TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.walletHoldRepository = walletHoldRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        Wallet wallet = new Wallet();
        wallet.setUserId(request.getUserId());
        wallet.setCurrency(request.getCurrency());
        wallet.setBalance(0L);
        wallet.setAvailableBalance(0L);

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet created successfully: userId={}, walletId={}",
                savedWallet.getUserId(), savedWallet.getId());
        return mapToResponse(savedWallet);
    }

    @Transactional
    public WalletResponse credit(CreditRequest request) {
        validateAmount(request.getAmount());
        Wallet wallet = getWalletForUpdate(request.getUserId(), request.getCurrency());

        wallet.setBalance(wallet.getBalance() + request.getAmount());
        wallet.setAvailableBalance(wallet.getAvailableBalance() + request.getAmount());
        walletRepository.save(wallet);

        transactionRepository.save(new Transaction(
                wallet, TransactionType.CREDIT, request.getAmount(),
                TransactionStatus.SUCCESS, UUID.randomUUID().toString()));

        log.info("CREDIT success: userId={}, amount={}, balance={}",
                request.getUserId(), request.getAmount(), wallet.getBalance());
        return mapToResponse(wallet);
    }

    @Transactional
    public WalletResponse debit(DebitRequest request) {
        validateAmount(request.getAmount());
        Wallet wallet = getWalletForUpdate(request.getUserId(), request.getCurrency());

        if (wallet.getAvailableBalance() < request.getAmount()) {
            transactionRepository.save(new Transaction(
                    wallet, TransactionType.DEBIT, request.getAmount(),
                    TransactionStatus.FAILED, UUID.randomUUID().toString()));
            log.warn("DEBIT failed: insufficient funds userId={}, amount={}",
                    request.getUserId(), request.getAmount());
            throw new InsufficientFundsException("Not enough balance");
        }

        wallet.setBalance(wallet.getBalance() - request.getAmount());
        wallet.setAvailableBalance(wallet.getAvailableBalance() - request.getAmount());
        walletRepository.save(wallet);

        transactionRepository.save(new Transaction(
                wallet, TransactionType.DEBIT, request.getAmount(),
                TransactionStatus.SUCCESS, UUID.randomUUID().toString()));

        log.info("DEBIT success: userId={}, amount={}, balance={}",
                request.getUserId(), request.getAmount(), wallet.getBalance());
        return mapToResponse(wallet);
    }

    @Transactional
    public WalletResponse getWallet(Long userId, String currency) {
        Wallet wallet = walletRepository
                .findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        return mapToResponse(wallet);
    }

    @Transactional
    public HoldResponse placeHold(HoldRequest request) {
        validateAmount(request.getAmount());
        Wallet wallet = getWalletForUpdate(request.getUserId(), request.getCurrency());

        if (wallet.getAvailableBalance() < request.getAmount()) {
            throw new InsufficientFundsException("Not enough balance to hold");
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance() - request.getAmount());

        WalletHold hold = new WalletHold();
        hold.setWallet(wallet);
        hold.setAmount(request.getAmount());
        hold.setHoldReference(generateHoldReference());
        hold.setStatus(HoldStatus.ACTIVE);
        hold.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(30));

        walletRepository.save(wallet);
        walletHoldRepository.save(hold);

        log.info("HOLD placed: userId={}, amount={}, holdRef={}",
                request.getUserId(), request.getAmount(), hold.getHoldReference());
        return new HoldResponse(hold.getHoldReference(), hold.getAmount(), hold.getStatus().name());
    }

    @Transactional
    public WalletResponse captureHold(CaptureRequest request) {
        WalletHold hold = walletHoldRepository
                .findByHoldReference(request.getHoldReference())
                .orElseThrow(() -> new NotFoundException("Hold not found"));

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new IllegalStateException("Hold is not active");
        }

        Wallet wallet = hold.getWallet();
        wallet.setBalance(wallet.getBalance() - hold.getAmount());
        hold.setStatus(HoldStatus.CAPTURED);

        walletRepository.save(wallet);
        walletHoldRepository.save(hold);

        log.info("HOLD captured: holdRef={}, amount={}", request.getHoldReference(), hold.getAmount());
        return mapToResponse(wallet);
    }

    @Transactional
    public HoldResponse releaseHold(String holdReference) {
        WalletHold hold = walletHoldRepository
                .findByHoldReference(holdReference)
                .orElseThrow(() -> new NotFoundException("Hold not found"));

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new IllegalStateException("Hold is not active");
        }

        Wallet wallet = hold.getWallet();
        wallet.setAvailableBalance(wallet.getAvailableBalance() + hold.getAmount());
        hold.setStatus(HoldStatus.RELEASED);

        walletRepository.save(wallet);
        walletHoldRepository.save(hold);

        log.info("HOLD released: holdRef={}, amount={}", holdReference, hold.getAmount());
        return new HoldResponse(hold.getHoldReference(), hold.getAmount(), hold.getStatus().name());
    }

    // FIX 4: Changed from private to package-private so Spring AOP proxy can
    // intercept @Transactional. A private @Transactional method is a no-op —
    // Spring cannot wrap it, so the pessimistic lock from
    // WalletRepository.findByUserIdAndCurrency was never applied.
    @Transactional
    Wallet getWalletForUpdate(Long userId, String currency) {
        return walletRepository
                .findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
    }

    private void validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
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

    @Transactional
    public java.util.List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }
}