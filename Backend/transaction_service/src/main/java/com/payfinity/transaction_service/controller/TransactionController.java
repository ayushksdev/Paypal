package com.payfinity.transaction_service.controller;


import com.payfinity.transaction_service.entity.Transaction;
import com.payfinity.transaction_service.service.TransactionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "https://payfinity-jgpd.vercel.app", "https://payfinity-jgpd.vercel.app/"})
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService service;
    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    public TransactionController(TransactionService service) {
        this.service = service;
    }
    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody Transaction transaction) {
        log.info("Create transaction request received from sender {} to receiver {} for amount {}", 
                transaction.getSenderId(), transaction.getReceiverId(), transaction.getAmount());

        Transaction created = service.createTransaction(transaction);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/user/{userId}")
public List<Transaction> getTransactionsByUser(@PathVariable Long userId) {
    log.info("Fetching transactions for user {}", userId);
    return service.getTransactionsByUser(userId);
}

    @GetMapping("/all")
    public List<Transaction> getAll() {
        log.info("Fetching all transactions");
        return service.getAllTransactions();
    }

}