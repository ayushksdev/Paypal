package com.paypal.user_service.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.paypal.user_service.entity.User;
import com.paypal.user_service.repository.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserServiceImp implements UserService {

    private final UserRepo userRepo;
    private static final Logger log = LoggerFactory.getLogger(UserServiceImp.class);

    public UserServiceImp(UserRepo userRepo){
        this.userRepo = userRepo;
    }

    @Override
    public User createUser(User user) {
        log.info("Saving new user to database");
        return userRepo.save(user);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        log.info("Querying user by ID: {}", id);
        return userRepo.findById(id);
    }

    @Override
    public List<User> getAllUsers() {
        log.info("Querying all users from database");
        return userRepo.findAll();
    }

    @Override
    public Optional<User> getUserByEmail(String email){
        log.info("Querying user by email: {}", email);
        return userRepo.findByEmail(email);
    }
}