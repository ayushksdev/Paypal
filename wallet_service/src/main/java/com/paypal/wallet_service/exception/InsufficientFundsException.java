package com.paypal.wallet_service.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(Long userId, Long amount) {
        super("Insufficient funds for userId=" + userId + ", amount=" + amount);
    }
}