package com.payfinity.wallet_service.controller;

import com.payfinity.wallet_service.dto.*;
import com.payfinity.wallet_service.service.WalletService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import com.payfinity.wallet_service.entity.Wallet;

@RestController
@CrossOrigin(origins = { "http://localhost:5173", "https://payfinity.vercel.app" })
@RequestMapping("/api/v1/wallets")
public class WalletController {

        private final WalletService walletService;
        private static final Logger log = LoggerFactory.getLogger(WalletController.class);

        // Constructor Injection
        public WalletController(WalletService walletService) {
                this.walletService = walletService;
        }

        // CREATE WALLET
        @PostMapping("/create")
        public ResponseEntity<WalletResponse> createWallet(
                        @RequestBody CreateWalletRequest request) {
                log.info("Create wallet request received for userId: {}", request.getUserId());

                WalletResponse response = walletService.createWallet(request);

                return ResponseEntity.ok(response);
        }

        // CREDIT MONEY
        @PostMapping("/credit")
        public ResponseEntity<WalletResponse> credit(
                        @RequestBody CreditRequest request) {
                log.info("Credit request received for userId: {}, amount: {}", request.getUserId(),
                                request.getAmount());

                WalletResponse response = walletService.credit(request);

                return ResponseEntity.ok(response);
        }

        // DEBIT MONEY
        @PostMapping("/debit")
        public ResponseEntity<WalletResponse> debit(
                        @RequestBody DebitRequest request) {
                log.info("Debit request received for userId: {}, amount: {}", request.getUserId(), request.getAmount());

                WalletResponse response = walletService.debit(request);

                return ResponseEntity.ok(response);
        }

        // GET WALLET DETAILS
        @GetMapping("/{userId}")
        public ResponseEntity<WalletResponse> getWallet(
                        @PathVariable Long userId,
                        @RequestParam(defaultValue = "INR") String currency) {
                log.info("Get wallet request received for userId: {}, currency: {}", userId, currency);

                WalletResponse response = walletService.getWallet(userId, currency);

                return ResponseEntity.ok(response);
        }

        // PLACE HOLD
        @PostMapping("/hold")
        public ResponseEntity<HoldResponse> placeHold(
                        @RequestBody HoldRequest request) {
                log.info("Place hold request received for userId: {}, amount: {}", request.getUserId(),
                                request.getAmount());

                HoldResponse response = walletService.placeHold(request);

                return ResponseEntity.ok(response);
        }

        // CAPTURE HOLD
        @PostMapping("/capture")
        public ResponseEntity<WalletResponse> captureHold(
                        @RequestBody CaptureRequest request) {
                log.info("Capture hold request received for hold reference: {}", request.getHoldReference());

                WalletResponse response = walletService.captureHold(request);

                return ResponseEntity.ok(response);
        }

        // RELEASE HOLD
        @PostMapping("/release/{holdReference}")
        public ResponseEntity<HoldResponse> releaseHold(
                        @PathVariable String holdReference) {
                log.info("Release hold request received for hold reference: {}", holdReference);

                HoldResponse response = walletService.releaseHold(holdReference);

                return ResponseEntity.ok(response);
        }

        // GET all WALLET
        @GetMapping("/all")
        public ResponseEntity<List<Wallet>> getAllWallets() {

                return ResponseEntity.ok(
                                walletService.getAllWallets());
        }
}