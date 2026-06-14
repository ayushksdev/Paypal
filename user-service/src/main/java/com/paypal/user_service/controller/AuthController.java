package com.paypal.user_service.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import com.paypal.user_service.dto.AuthResponse;
import com.paypal.user_service.dto.LoginRequest;
import com.paypal.user_service.dto.SignupRequest;
import com.paypal.user_service.entity.User;
import com.paypal.user_service.service.UserServiceImp;
import com.paypal.user_service.util.JWTUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserServiceImp userService;
    private final JWTUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

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

        Optional<User> existingUser =
                userService.getUserByEmail(request.getEmail());

        if (existingUser.isPresent()) {
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

        AuthResponse response = new AuthResponse(
                "✅ User registered successfully",
                savedUser.getId(),
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        Optional<User> optUser =
                userService.getUserByEmail(request.getEmail());

        if (optUser.isEmpty()) {
            return ResponseEntity.status(401)
                    .body("❌ User not found");
        }

        User user = optUser.get();

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            return ResponseEntity.status(401)
                    .body("❌ Invalid credentials");
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        AuthResponse response = new AuthResponse(
                "✅ Login successful",
                user.getId(),
                token
        );

        return ResponseEntity.ok(response);
    }
}