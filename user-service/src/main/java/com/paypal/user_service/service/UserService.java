package com.paypal.user_service.service;

import java.util.List;
import java.util.Optional;

import com.paypal.user_service.entity.User;

public interface UserService {
    
    User createUser(User user);

    Optional<User> getUserById(Long id);

    List<User> getAllUsers();

    Optional<User> getUserByEmail(String email);
}