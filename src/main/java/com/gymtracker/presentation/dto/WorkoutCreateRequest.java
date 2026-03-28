package com.gymtracker.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;

/**
 * WorkoutCreateRequest - sent by client when creating a new workout
 * 
 * A workout contains one or more exercises, each with one or more sets.
 * All must be provided at creation time (domain invariants prevent empty
 * workouts/exercises).
 * 
 * Fields:
 * - date: Date of workout (cannot be in future, must be today or earlier)
 * - title: Workout name (e.g., "Chest Day", "Push/Pull", marked @NotBlank)
 * - notes: Optional notes (e.g., "Felt strong today", can be empty string)
 * - exercises: At least one exercise with at least one set each
 * 
 * Why @Valid on exercises list? Cascades validation to nested
 * ExerciseCreateRequest objects
 * 
 * Example request body:
 * {
 * "date": "2026-03-15",
 * "title": "Chest Day",
 * "notes": "Morning session",
 * "exercises": [
 * {
 * "name": "Bench Press",
 * "category": "CHEST",
 * "sets": [
 * { "reps": 8, "weight": 80 },
 * { "reps": 8, "weight": 80 },
 * { "reps": 8, "weight": 80 }
 * ]
 * },
 * {
 * "name": "Incline Dumbbell Press",
 * "category": "CHEST",
 * "sets": [
 * { "reps": 10, "weight": 35 },
 * { "reps": 10, "weight": 35 }
 * ]
 * }
 * ]
 * }
 */
public record WorkoutCreateRequest(
        @NotNull(message = "Date is required") @PastOrPresent(message = "Workout date cannot be in the future") LocalDate date,

        @NotBlank(message = "Workout title is required") String title,

        String notes,

        @NotEmpty(message = "Workout must have at least one exercise") @Valid List<ExerciseCreateRequest> exercises) {
}
