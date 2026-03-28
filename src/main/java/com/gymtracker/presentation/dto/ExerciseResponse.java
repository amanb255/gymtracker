package com.gymtracker.presentation.dto;

import java.util.List;
import java.util.UUID;

/**
 * ExerciseResponse - sent to client when retrieving an exercise
 * 
 * Contains complete exercise data including all sets
 * 
 * Fields:
 * - id: Unique identifier for this exercise (UUID)
 * - name: Exercise name (e.g., "Bench Press")
 * - category: Exercise category as string uppercase (e.g., "CHEST")
 * - sets: List of all sets in this exercise (with IDs for later
 * updates/deletes)
 * 
 * Example response:
 * {
 * "id": "550e8400-e29b-41d4-a716-446655440000",
 * "name": "Bench Press",
 * "category": "CHEST",
 * "sets": [
 * { "id": "123e4567-e89b-12d3-a456-426614174000", "reps": 8, "weight": 80.0 },
 * { "id": "223e4567-e89b-12d3-a456-426614174000", "reps": 8, "weight": 80.0 },
 * { "id": "323e4567-e89b-12d3-a456-426614174000", "reps": 8, "weight": 80.0 }
 * ]
 * }
 */
public record ExerciseResponse(
        UUID id,
        String name,
        String category,
        List<SetEntryResponse> sets) {
}
