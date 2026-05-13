package com.paypal.wallet_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateWalletRequest {

    @NotNull(message = "UserId is required")
    private Long userId;

    @NotBlank(message = "Currency is required")
    private String currency;

    public CreateWalletRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}