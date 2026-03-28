package com.gymtracker.presentation.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/**
 * SetEntryRequest - sent by client when creating or updating a set entry
 * 
 * A set entry represents one set of an exercise (e.g., "10 reps at 50kg")
 * 
 * Fields:
 * - reps: Number of repetitions (must be > 0)
 * - weight: Weight in kg (must be >= 0, e.g., bodyweight exercise = 0kg)
 * 
 * Used by: POST /api/workouts/{id}/exercises/{exId}/sets
 * PATCH /api/workouts/{id}/exercises/{exId}/sets/{setId}
 */
public record SetEntryRequest(
        @Positive(message = "Reps must be greater than 0") Integer reps,

        @PositiveOrZero(message = "Weight cannot be negative") Double weight) {
}
