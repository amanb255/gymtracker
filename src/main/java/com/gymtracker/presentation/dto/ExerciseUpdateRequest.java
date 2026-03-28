package com.gymtracker.presentation.dto;

import jakarta.validation.constraints.Pattern;

/**
 * ExerciseUpdateRequest - sent by client when updating an exercise
 * 
 * All fields optional - client might update just name, just category, or both
 * 
 * Fields:
 * - name: New exercise name (optional, blank means no change)
 * - category: New exercise category (optional, null means no change)
 * 
 * Example: Update just the name
 * {
 * "name": "Incline Bench Press",
 * "category": null
 * }
 */
public record ExerciseUpdateRequest(
        String name,

        @Pattern(regexp = "^(CHEST|BACK|LEGS|SHOULDERS|TRICEPS|BICEPS|ABS|CARDIO|HIIT|OTHER)$", message = "Category must be one of: CHEST, BACK, LEGS, SHOULDERS, TRICEPS, BICEPS, ABS, CARDIO, HIIT, OTHER") String category) {
}
