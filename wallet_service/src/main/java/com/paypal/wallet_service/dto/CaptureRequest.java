package com.paypal.wallet_service.dto;

import jakarta.validation.constraints.NotBlank;

public class CaptureRequest {

    @NotBlank(message = "Hold reference is required")
    private String holdReference;

    public CaptureRequest() {}

    public String getHoldReference() { return holdReference; }
    public void setHoldReference(String holdReference) { this.holdReference = holdReference; }
}