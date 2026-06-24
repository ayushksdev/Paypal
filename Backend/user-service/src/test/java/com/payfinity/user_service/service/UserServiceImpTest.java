package com.payfinity.user_service.service;

import com.payfinity.user_service.entity.User;
import com.payfinity.user_service.repository.UserRepo;
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
public class UserServiceImpTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserServiceImp userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@test.com");
        user.setPassword("hashedpassword");
        user.setRole("ROLE_USER");
    }

    @Test
    void testCreateUser() {
        when(userRepo.save(any(User.class))).thenReturn(user);

        User created = userService.createUser(user);

        assertNotNull(created);
        assertEquals("test@test.com", created.getEmail());
        verify(userRepo, times(1)).save(user);
    }

    @Test
    void testGetUserByEmail() {
        when(userRepo.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        Optional<User> found = userService.getUserByEmail("test@test.com");

        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
        verify(userRepo, times(1)).findByEmail("test@test.com");
    }
}
