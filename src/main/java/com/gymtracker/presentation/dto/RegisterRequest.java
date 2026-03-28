package com.gymtracker.presentation.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Name is required") String name,

        @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email,

        @NotBlank(message = "Password is required") @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters") String password,

        @NotNull(message = "Date of birth is required") @PastOrPresent(message = "Date of birth cannot be in the future") LocalDate dateOfBirth,

        @NotNull(message = "Height is required") @Positive(message = "Height must be positive") Integer heightCm,

        @NotNull(message = "Gender is required") String gender) {
}
