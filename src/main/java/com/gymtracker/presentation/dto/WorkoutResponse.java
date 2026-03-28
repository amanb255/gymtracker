package com.gymtracker.presentation.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * WorkoutResponse - sent to client when retrieving a workout
 * 
 * Contains complete workout data with all exercises and all sets
 * This is the most complete representation of a workout
 * 
 * Fields:
 * - id: Unique identifier for this workout (UUID)
 * - userId: Owner of this workout (the logged-in user)
 * - date: Date this workout was performed
 * - title: Workout title
 * - notes: Optional notes about the workout
 * - exercises: Complete list of exercises with all their sets
 * 
 * Example response:
 * {
 * "id": "550e8400-e29b-41d4-a716-446655440000",
 * "userId": "123e4567-e89b-12d3-a456-426614174000",
 * "date": "2026-03-15",
 * "title": "Chest Day",
 * "notes": "Morning session",
 * "exercises": [
 * {
 * "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
 * "name": "Bench Press",
 * "category": "CHEST",
 * "sets": [
 * { "id": "...", "reps": 8, "weight": 80.0 },
 * { "id": "...", "reps": 8, "weight": 80.0 }
 * ]
 * }
 * ]
 * }
 */
public record WorkoutResponse(
        UUID id,
        UUID userId,
        LocalDate date,
        String title,
        String notes,
        List<ExerciseResponse> exercises) {
}
