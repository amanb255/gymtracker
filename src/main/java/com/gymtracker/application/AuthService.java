package com.gymtracker.application;

import com.gymtracker.domain.model.Gender;
import com.gymtracker.domain.model.User;
import com.gymtracker.domain.repository.UserRepository;
import com.gymtracker.infrastructure.security.JwtTokenProvider;
import com.gymtracker.presentation.dto.AuthResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Register a new user with email and password
     */
    public AuthResponse register(String name, String email, String password, LocalDate dateOfBirth, Integer heightCm,
            Gender gender) {
        // Check if email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Hash the plain-text password
        String hashedPassword = passwordEncoder.encode(password);

        // Create and save new user
        User user = new User(name, email, hashedPassword, dateOfBirth, heightCm, gender);
        User savedUser = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId(), savedUser.getEmail());

        return new AuthResponse(
                savedUser.getId(),
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds());
    }

    /**
     * Login with email and password
     */
    public AuthResponse login(String email, String password) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(password, user.getHashedPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        return new AuthResponse(
                user.getId(),
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds());
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        // Extract user info from token
        UUID userId = jwtTokenProvider.extractUserId(refreshToken);
        String email = jwtTokenProvider.extractEmail(refreshToken);

        // Verify user still exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());

        return new AuthResponse(
                user.getId(),
                newAccessToken,
                refreshToken, // Return same refresh token
                jwtTokenProvider.getAccessTokenExpirationSeconds());
    }
}
