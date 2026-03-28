package com.gymtracker.presentation.controller;

import com.gymtracker.application.AuthService;
import com.gymtracker.domain.model.Gender;
import com.gymtracker.presentation.dto.AuthResponse;
import com.gymtracker.presentation.dto.LoginRequest;
import com.gymtracker.presentation.dto.RefreshTokenRequest;
import com.gymtracker.presentation.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user with email and password
     * 
     * Endpoint: POST /api/auth/register
     * Validates RegisterRequest, creates new user, hashes password, generates JWT
     * tokens
     * Returns 201 Created with AuthResponse containing userId, accessToken,
     * refreshToken, expiresIn
     * 
     * Request body example:
     * {
     * "name": "John Doe",
     * "email": "john@example.com",
     * "password": "SecurePass123",
     * "dateOfBirth": "1990-05-15",
     * "heightCm": 180,
     * "gender": "MALE"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(
                request.name(),
                request.email(),
                request.password(),
                request.dateOfBirth(),
                request.heightCm(),
                Gender.valueOf(request.gender().toUpperCase()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login with email and password
     * 
     * Endpoint: POST /api/auth/login
     * Validates LoginRequest, finds user by email, verifies password, generates JWT
     * tokens
     * Returns 200 OK with AuthResponse containing userId, accessToken,
     * refreshToken, expiresIn
     * 
     * Request body example:
     * {
     * "email": "john@example.com",
     * "password": "SecurePass123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request.email(), request.password());
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh the access token using a valid refresh token
     * 
     * Endpoint: POST /api/auth/refresh
     * Validates RefreshTokenRequest, extracts user info from refresh token,
     * generates new access token
     * Returns 200 OK with AuthResponse containing new accessToken (same
     * refreshToken)
     * 
     * Request body example:
     * {
     * "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI..."
     * }
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }
}
