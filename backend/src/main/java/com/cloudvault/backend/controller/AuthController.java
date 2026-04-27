package com.cloudvault.backend.controller;

import com.cloudvault.backend.dto.AuthRequest;
import com.cloudvault.backend.dto.AuthResponse;
import com.cloudvault.backend.dto.RegisterRequest;
import com.cloudvault.backend.dto.UserInfoResponse;
import com.cloudvault.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller.
 * <ul>
 *   <li>POST /api/auth/register — create a new account</li>
 *   <li>POST /api/auth/login    — authenticate and receive JWT</li>
 *   <li>GET  /api/auth/me       — get current user info (requires auth)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser() {
        UserInfoResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(response);
    }
}
