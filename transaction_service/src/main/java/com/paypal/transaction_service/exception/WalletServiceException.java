package com.paypal.transaction_service.exception;

public class WalletServiceException extends RuntimeException {

    public WalletServiceException(String message) {
        super(message);
    }
}