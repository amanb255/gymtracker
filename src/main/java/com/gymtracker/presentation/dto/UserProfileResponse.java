package com.gymtracker.presentation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserProfileResponse - sent to client when they request GET /api/users/profile
 * Contains all user profile information except hashed password
 * 
 * Fields:
 * - id: The unique identifier for the user
 * - name: User's full name
 * - email: User's email (unique per system)
 * - heightCm: User's height in centimeters
 * - dateOfBirth: User's date of birth
 * - gender: User's gender (MALE, FEMALE, OTHER)
 * - createdAt: When user account was created
 * - updatedAt: When user profile was last updated
 */
public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        Integer heightCm,
        LocalDate dateOfBirth,
        String gender,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
