package com.paypal.wallet_service.entity;

import jakarta.persistence.*;

// FIX 6: Removed duplicate createdAt/updatedAt fields.
// Wallet extends Auditable which already maps these columns
// via @CreatedDate / @LastModifiedDate + Spring Data JPA Auditing.
// Having them declared again in Wallet caused Hibernate to create
// duplicate columns and write conflicting values.

@Entity
@Table(name = "wallets")
public class Wallet extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(nullable = false)
    private Long balance = 0L;

    @Column(nullable = false)
    private Long availableBalance = 0L;

    // ← createdAt and updatedAt REMOVED — they are inherited from Auditable
    //    and managed automatically by @EnableJpaAuditing in WalletServiceApplication

    public Wallet() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getBalance() { return balance; }
    public void setBalance(Long balance) { this.balance = balance; }

    public Long getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(Long availableBalance) { this.availableBalance = availableBalance; }
}