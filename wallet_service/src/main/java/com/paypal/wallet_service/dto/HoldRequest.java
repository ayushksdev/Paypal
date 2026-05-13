package com.paypal.wallet_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class HoldRequest {

    @NotNull(message = "UserId is required")
    private Long userId;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Long amount;

    public HoldRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
}