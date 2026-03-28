package com.gymtracker.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * ExerciseCreateRequest - sent by client when adding an exercise to a workout
 * 
 * Why require sets at creation time? Domain invariant: Exercises must have ≥1
 * set.
 * This prevents creating empty exercises then forgetting to add sets.
 * 
 * Fields:
 * - name: Exercise name (e.g., "Bench Press", must not be blank)
 * - category: Exercise category enum string (e.g., "CHEST", "BACK", converted
 * from string)
 * - sets: At least one set entry (marked with @Valid for cascading validation)
 * 
 * Example request body:
 * {
 * "name": "Bench Press",
 * "category": "CHEST",
 * "sets": [
 * { "reps": 8, "weight": 80 },
 * { "reps": 8, "weight": 80 },
 * { "reps": 8, "weight": 80 }
 * ]
 * }
 */
public record ExerciseCreateRequest(
        @NotBlank(message = "Exercise name is required") String name,

        @NotBlank(message = "Category is required") @Pattern(regexp = "^(CHEST|BACK|LEGS|SHOULDERS|TRICEPS|BICEPS|ABS|CARDIO|HIIT|OTHER)$", message = "Category must be one of: CHEST, BACK, LEGS, SHOULDERS, TRICEPS, BICEPS, ABS, CARDIO, HIIT, OTHER") String category,

        @NotEmpty(message = "Exercise must have at least one set") @Valid List<SetEntryRequest> sets) {
}
