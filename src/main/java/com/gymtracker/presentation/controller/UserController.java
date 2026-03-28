package com.gymtracker.presentation.controller;

import com.gymtracker.application.UserService;
import com.gymtracker.domain.model.Gender;
import com.gymtracker.domain.model.User;
import com.gymtracker.presentation.dto.UserProfileResponse;
import com.gymtracker.presentation.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get the authenticated user's profile information
     * 
     * Endpoint: GET /api/users/profile
     * Requires authentication (JWT token in Authorization header)
     * Spring Security extracts userId from JWT token and passes via Authentication
     * object
     * Returns 200 OK with UserProfileResponse containing name, email, heightCm,
     * dateOfBirth, gender, createdAt, updatedAt
     * 
     * Response example:
     * {
     * "id": "550e8400-e29b-41d4-a716-446655440000",
     * "name": "John Doe",
     * "email": "john@example.com",
     * "heightCm": 180,
     * "dateOfBirth": "1990-05-15",
     * "gender": "MALE",
     * "createdAt": "2026-03-15T10:30:00",
     * "updatedAt": "2026-03-15T10:30:00"
     * }
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        // Authentication.getPrincipal() returns the userId from JWT token (set by
        // JwtAuthenticationFilter)
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getHeightCm(),
                user.getDateOfBirth(),
                user.getGender().toString(),
                user.getCreatedAt(),
                user.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * Update the authenticated user's profile information
     * 
     * Endpoint: PATCH /api/users/profile
     * Requires authentication (JWT token in Authorization header)
     * Validates UserUpdateRequest, updates only provided fields (name, heightCm, or
     * gender)
     * Returns 200 OK with updated UserProfileResponse
     * 
     * Request body example (all fields optional):
     * {
     * "name": "Jane Doe",
     * "heightCm": 175,
     * "gender": "FEMALE"
     * }
     */
    @PatchMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequest request) {

        // Extract userId from JWT token
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        // Build gender enum if provided
        Gender gender = request.gender() != null ? Gender.valueOf(request.gender().toUpperCase()) : null;

        // Call service to update user
        User updatedUser = userService.updateUser(userId, request.name(), request.heightCm(), gender);

        UserProfileResponse response = new UserProfileResponse(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getHeightCm(),
                updatedUser.getDateOfBirth(),
                updatedUser.getGender().toString(),
                updatedUser.getCreatedAt(),
                updatedUser.getUpdatedAt());

        return ResponseEntity.ok(response);
    }
}
