package com.gymtracker.presentation.dto;

import java.util.UUID;

/**
 * SetEntryResponse - sent to client when retrieving a set entry
 * 
 * This is the response format for a single set. Client receives:
 * - The ID (so they can update/delete it later)
 * - The reps and weight they originally set
 * 
 * Fields:
 * - id: Unique identifier for this set (UUID)
 * - reps: Number of repetitions
 * - weight: Weight in kg used
 * 
 * Returned by: GET /api/workouts/{id} (includes all sets in exercises)
 * POST /api/workouts/{id}/exercises/{exId}/sets (after creation)
 * PATCH /api/workouts/{id}/exercises/{exId}/sets/{setId} (after update)
 */
public record SetEntryResponse(
        UUID id,
        Integer reps,
        Double weight) {
}
