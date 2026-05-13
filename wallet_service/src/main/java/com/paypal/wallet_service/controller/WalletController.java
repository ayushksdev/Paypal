package com.paypal.wallet_service.controller;

import com.paypal.wallet_service.dto.*;
import com.paypal.wallet_service.service.WalletService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    // Constructor Injection
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    // CREATE WALLET
    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @RequestBody CreateWalletRequest request
    ) {

        WalletResponse response =
                walletService.createWallet(request);

        return ResponseEntity.ok(response);
    }

    // CREDIT MONEY
    @PostMapping("/credit")
    public ResponseEntity<WalletResponse> credit(
            @RequestBody CreditRequest request
    ) {

        WalletResponse response =
                walletService.credit(request);

        return ResponseEntity.ok(response);
    }

    // DEBIT MONEY
    @PostMapping("/debit")
    public ResponseEntity<WalletResponse> debit(
            @RequestBody DebitRequest request
    ) {

        WalletResponse response =
                walletService.debit(request);

        return ResponseEntity.ok(response);
    }

    // GET WALLET DETAILS
    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWallet(
            @PathVariable Long userId,
            @RequestParam String currency
    ) {

        WalletResponse response =
                walletService.getWallet(userId, currency);

        return ResponseEntity.ok(response);
    }

    // PLACE HOLD
    @PostMapping("/hold")
    public ResponseEntity<HoldResponse> placeHold(
            @RequestBody HoldRequest request
    ) {

        HoldResponse response =
                walletService.placeHold(request);

        return ResponseEntity.ok(response);
    }

    // CAPTURE HOLD
    @PostMapping("/capture")
    public ResponseEntity<WalletResponse> captureHold(
            @RequestBody CaptureRequest request
    ) {

        WalletResponse response =
                walletService.captureHold(request);

        return ResponseEntity.ok(response);
    }

    // RELEASE HOLD
    @PostMapping("/release/{holdReference}")
    public ResponseEntity<HoldResponse> releaseHold(
            @PathVariable String holdReference
    ) {

        HoldResponse response =
                walletService.releaseHold(holdReference);

        return ResponseEntity.ok(response);
    }
}