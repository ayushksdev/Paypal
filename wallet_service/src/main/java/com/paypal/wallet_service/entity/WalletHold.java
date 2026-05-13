package com.paypal.wallet_service.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_holds")
public class WalletHold extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(nullable = false, unique = true)
    private String holdReference;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status = HoldStatus.ACTIVE;

    // ✅ EXPIRY FIELD (IMPORTANT)
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public WalletHold() {}

    // ================= GETTERS =================

    public Long getId() {
        return id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public String getHoldReference() {
        return holdReference;
    }

    public Long getAmount() {
        return amount;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    // ================= SETTERS =================

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public void setHoldReference(String holdReference) {
        this.holdReference = holdReference;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public void setStatus(HoldStatus status) {
        this.status = status;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}