package com.paypal.wallet_service.service;

import com.paypal.wallet_service.dto.*;
import com.paypal.wallet_service.entity.*;
import com.paypal.wallet_service.exception.*;
import com.paypal.wallet_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletHoldRepository walletHoldRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUserId(100L);
        wallet.setCurrency("INR");
        wallet.setBalance(500L);
        wallet.setAvailableBalance(500L);
    }

    @Test
    void testCredit() {
        when(walletRepository.findByUserIdAndCurrency(100L, "INR")).thenReturn(Optional.of(wallet));

        CreditRequest request = new CreditRequest();
        request.setUserId(100L);
        request.setCurrency("INR");
        request.setAmount(100L);

        WalletResponse response = walletService.credit(request);

        assertNotNull(response);
        assertEquals(600L, response.getBalance());
        assertEquals(600L, response.getAvailableBalance());
        verify(walletRepository, times(1)).save(wallet);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testDebitSuccess() {
        when(walletRepository.findByUserIdAndCurrency(100L, "INR")).thenReturn(Optional.of(wallet));

        DebitRequest request = new DebitRequest();
        request.setUserId(100L);
        request.setCurrency("INR");
        request.setAmount(100L);

        WalletResponse response = walletService.debit(request);

        assertNotNull(response);
        assertEquals(400L, response.getBalance());
        assertEquals(400L, response.getAvailableBalance());
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void testDebitInsufficientFunds() {
        when(walletRepository.findByUserIdAndCurrency(100L, "INR")).thenReturn(Optional.of(wallet));

        DebitRequest request = new DebitRequest();
        request.setUserId(100L);
        request.setCurrency("INR");
        request.setAmount(600L); // More than 500

        assertThrows(InsufficientFundsException.class, () -> walletService.debit(request));
    }
}
