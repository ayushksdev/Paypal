package com.payfinity.user_service.dto;

public class AuthResponse {

    private String message;
    private Long userId;
    private String token;

    public AuthResponse() {
    }

    public AuthResponse(String message, Long userId, String token) {
        this.message = message;
        this.userId = userId;
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}