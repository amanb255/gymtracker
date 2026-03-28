package com.gymtracker.presentation.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * UserUpdateRequest - received from client on PATCH /api/users/profile
 * All fields are optional - client only sends fields they want to update
 * 
 * Fields:
 * - name: Optional new name (must not be blank if provided)
 * - heightCm: Optional new height in cm (must be positive if provided)
 * - gender: Optional new gender (MALE, FEMALE, or OTHER if provided)
 */
public record UserUpdateRequest(
        @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters") String name,

        @Positive(message = "Height must be positive") Integer heightCm,

        String gender) {
}
