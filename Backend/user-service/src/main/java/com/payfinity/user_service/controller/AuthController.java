package com.payfinity.user_service.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;

import com.payfinity.user_service.dto.AuthResponse;
import com.payfinity.user_service.dto.LoginRequest;
import com.payfinity.user_service.dto.SignupRequest;
import com.payfinity.user_service.entity.User;
import com.payfinity.user_service.service.UserServiceImp;
import com.payfinity.user_service.util.JWTUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = {"http://localhost:5173", "https://payfinity.vercel.app"})
@RequestMapping("/auth")
public class AuthController {

    private final UserServiceImp userService;
    private final JWTUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @org.springframework.beans.factory.annotation.Value("${wallet.service.url}")
    private String walletServiceUrl;

    public AuthController(
            UserServiceImp userService,
            JWTUtil jwtUtil,
            PasswordEncoder passwordEncoder) {

        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        log.info("Signup request received for email: {}", request.getEmail());

        Optional<User> existingUser =
                userService.getUserByEmail(request.getEmail());

        if (existingUser.isPresent()) {
            log.warn("Signup failed: User already exists for email: {}", request.getEmail());
            return ResponseEntity.badRequest()
                    .body("⚠️ User already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole("ROLE_USER");
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userService.createUser(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Automatically create wallet for the user in wallet service
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            java.util.Map<String, Object> walletRequest = new java.util.HashMap<>();
            walletRequest.put("userId", savedUser.getId());
            walletRequest.put("currency", "INR");
            
            restTemplate.postForEntity(this.walletServiceUrl, walletRequest, String.class);
            log.info("Wallet automatically created successfully for user ID: {}", savedUser.getId());
        } catch (Exception e) {
            log.error("Failed to automatically create wallet for user ID: {}. Error: {}", savedUser.getId(), e.getMessage());
        }

        String token = jwtUtil.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        AuthResponse response = new AuthResponse(
                "✅ User registered successfully",
                savedUser.getId(),
                token
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());

        Optional<User> optUser =
                userService.getUserByEmail(request.getEmail());

        if (optUser.isEmpty()) {
            log.warn("Login failed: User not found for email: {}", request.getEmail());
            return ResponseEntity.status(401)
                    .body("❌ User not found");
        }

        User user = optUser.get();

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {
            
            log.warn("Login failed: Invalid credentials for email: {}", request.getEmail());
            return ResponseEntity.status(401)
                    .body("❌ Invalid credentials");
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
        log.info("Login successful for user ID: {}", user.getId());

        AuthResponse response = new AuthResponse(
                "✅ Login successful",
                user.getId(),
                token
        );

        return ResponseEntity.ok(response);
    }
}