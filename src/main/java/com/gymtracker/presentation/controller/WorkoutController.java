package com.gymtracker.presentation.controller;

import com.gymtracker.application.WorkoutService;
import com.gymtracker.domain.model.*;
import com.gymtracker.presentation.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workouts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    /**
     * ENDPOINT 1: Create a new workout
     * 
     * POST /api/workouts
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Client sends workout with date, title, notes, and exercises (each with
     * sets)
     * 2. Spring validates all nested fields (@Valid cascades through exercise/set
     * validations)
     * 3. Service creates Workout entity + Exercise entities + SetEntry entities
     * 4. All saved in single transaction (atomic - all or nothing)
     * 5. Returns 201 Created with full WorkoutResponse (includes IDs for future
     * updates)
     * 
     * Business Rules:
     * - Cannot create workout without exercises (domain invariant)
     * - Cannot create exercise without sets (domain invariant)
     * - Cannot create workout with duplicate exercise names
     * 
     * Example response:
     * {
     * "id": "550e8400-e29b-41d4-a716-446655440000",
     * "userId": "extracted-from-jwt",
     * "date": "2026-03-15",
     * "title": "Chest Day",
     * "notes": "Morning",
     * "exercises": [...]
     * }
     */
    @PostMapping
    public ResponseEntity<WorkoutResponse> createWorkout(
            Authentication authentication,
            @Valid @RequestBody WorkoutCreateRequest request) {

        // Extract userId from JWT token (set by JwtAuthenticationFilter)
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        // Convert DTOs to domain entities
        // This is CRITICAL: DTOs are just data containers, we need real domain entities
        List<Exercise> exercises = request.exercises().stream()
                .map(exRequest -> new Exercise(
                        exRequest.name(),
                        ExerciseCategory.valueOf(exRequest.category().toUpperCase()),
                        exRequest.sets().stream()
                                .map(setRequest -> new SetEntry(setRequest.reps(), setRequest.weight()))
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());

        // Create workout in service (handles user lookup, repository save, etc.)
        Workout workout = workoutService.createWorkout(userId, request.date(), request.title(), request.notes(),
                exercises);

        // Convert saved domain entity back to response DTO
        WorkoutResponse response = workoutToResponse(workout);

        // Return 201 Created (not 200 - we created a new resource)
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ENDPOINT 2: Get all workouts for authenticated user
     * 
     * GET /api/workouts
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Extracts userId from JWT
     * 2. Fetches all workouts belonging to this user (others can't see your
     * workouts)
     * 3. Returns list in reverse chronological order (newest first)
     * 4. Returns 200 OK with list of WorkoutResponse
     * 
     * Note: Results are ordered by date DESC (most recent first)
     * 
     * Response example:
     * [
     * {
     * "id": "550e8400-e29b-41d4-a716-446655440000",
     * "userId": "user-id",
     * "date": "2026-03-15",
     * "title": "Chest Day",
     * ...
     * }
     * ]
     */
    @GetMapping
    public ResponseEntity<List<WorkoutResponse>> getWorkouts(Authentication authentication) {
        // Extract userId from JWT
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        // Fetch all workouts for user
        List<Workout> workouts = workoutService.getWorkoutsForUser(userId);

        // Convert all to response DTOs
        List<WorkoutResponse> responses = workouts.stream()
                .map(this::workoutToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * ENDPOINT 3: Get specific workout by ID
     * 
     * GET /api/workouts/{id}
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Extracts userId from JWT
     * 2. Fetches workout by ID
     * 3. Verifies workout belongs to authenticated user (security check!)
     * 4. Returns 200 OK with full WorkoutResponse (includes all exercises/sets)
     * 5. Returns 404 Not Found if user doesn't own this workout
     * 
     * Security note: User can only view/modify their own workouts
     * 
     * Path parameters:
     * - id: UUID of workout to fetch
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkoutResponse> getWorkout(
            Authentication authentication,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        Workout workout = workoutService.getWorkoutById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        // Security check: ensure workout belongs to authenticated user
        if (!workout.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Workout not found");
        }

        WorkoutResponse response = workoutToResponse(workout);
        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT 4: Update workout (title and/or notes only)
     * 
     * PATCH /api/workouts/{id}
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Extracts userId from JWT
     * 2. Verifies workout belongs to user
     * 3. Updates title and/or notes (all fields optional)
     * 4. Returns 200 OK with updated WorkoutResponse
     * 
     * IMPORTANT: Cannot update date or exercises via this endpoint
     * - Date is set once (historical accuracy)
     * - Exercises modified via separate /exercises endpoints
     * 
     * Request body (all optional):
     * {
     * "title": "New Title",
     * "notes": "Updated notes"
     * }
     */
    @PatchMapping("/{id}")
    public ResponseEntity<WorkoutResponse> updateWorkout(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody WorkoutUpdateRequest request) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        // Verify ownership
        Workout existingWorkout = workoutService.getWorkoutById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        if (!existingWorkout.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Workout not found");
        }

        // Update and save
        Workout updated = workoutService.updateWorkout(id, request.title(), request.notes());

        WorkoutResponse response = workoutToResponse(updated);
        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT 5: Delete workout
     * 
     * DELETE /api/workouts/{id}
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Extracts userId from JWT
     * 2. Verifies workout belongs to user
     * 3. Deletes workout AND all related exercises and sets (cascade delete)
     * 4. Returns 204 No Content (successful deletion, no response body)
     * 
     * Note: Cascade delete = removing workout automatically removes:
     * - All Exercise entities in this workout
     * - All SetEntry entities in those exercises
     * 
     * Path parameters:
     * - id: UUID of workout to delete
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkout(
            Authentication authentication,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        Workout workout = workoutService.getWorkoutById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        if (!workout.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Workout not found");
        }

        workoutService.deleteWorkout(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * ENDPOINT 6: Add exercise to existing workout
     * 
     * POST /api/workouts/{id}/exercises
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Takes existing workout
     * 2. Adds new exercise with initial set entries
     * 3. Verifies no duplicate exercise names in same workout (business rule)
     * 4. Returns 201 Created with updated WorkoutResponse (full workout data)
     * 
     * Why separate endpoint? Allows adding exercises after workout creation
     * Client might create workout, then add more exercises during/after session
     * 
     * Path parameters:
     * - id: UUID of workout to modify
     * 
     * Request body:
     * {
     * "name": "Incline Bench Press",
     * "category": "CHEST",
     * "sets": [
     * { "reps": 8, "weight": 70 },
     * { "reps": 8, "weight": 70 }
     * ]
     * }
     */
    @PostMapping("/{id}/exercises")
    public ResponseEntity<WorkoutResponse> addExerciseToWorkout(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody ExerciseCreateRequest request) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        Workout workout = workoutService.getWorkoutById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        if (!workout.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Workout not found");
        }

        // Create Exercise entity from DTO
        Exercise exercise = new Exercise(
                request.name(),
                ExerciseCategory.valueOf(request.category().toUpperCase()),
                request.sets().stream()
                        .map(setRequest -> new SetEntry(setRequest.reps(), setRequest.weight()))
                        .collect(Collectors.toList()));

        workoutService.addExerciseToWorkout(id, exercise);

        // Return updated workout
        Workout updatedWorkout = workoutService.getWorkoutById(id)
                .orElseThrow();

        WorkoutResponse response = workoutToResponse(updatedWorkout);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ENDPOINT 7: Update exercise (name and/or category)
     * 
     * PATCH /api/workouts/{workoutId}/exercises/{exerciseId}
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Finds exercise within specific workout
     * 2. Updates name and/or category (all fields optional)
     * 3. Returns 200 OK with updated WorkoutResponse
     * 
     * Note: Cannot update individual sets here - use separate /sets endpoints
     * 
     * Path parameters:
     * - workoutId: UUID of workout
     * - exerciseId: UUID of exercise within that workout
     * 
     * Request body (all optional):
     * {
     * "name": "Barbell Bench Press",
     * "category": "CHEST"
     * }
     */
    @PatchMapping("/{workoutId}/exercises/{exerciseId}")
    public ResponseEntity<WorkoutResponse> updateExercise(
            Authentication authentication,
            @PathVariable UUID workoutId,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody ExerciseUpdateRequest request) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        Workout workout = workoutService.getWorkoutById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        if (!workout.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Workout not found");
        }

        workoutService.updateExercise(workoutId, exerciseId, request.name(), request.category());

        Workout updatedWorkout = workoutService.getWorkoutById(workoutId)
                .orElseThrow();

        WorkoutResponse response = workoutToResponse(updatedWorkout);
        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT 8: Delete exercise from workout
     * 
     * DELETE /api/workouts/{workoutId}/exercises/{exerciseId}
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Finds exercise within workout
     * 2. Removes exercise AND all its sets (cascade delete)
     * 3. Returns 204 No Content
     * 
     * BUSINESS RULE: Cannot delete last exercise (workout must have ≥1 exercise)
     * If user tries to delete last exercise, gets error "Workout must contain at
     * least one exercise"
     * 
     * Path parameters:
     * - workoutId: UUID of workout
     * - exerciseId: UUID of exercise to remove
     */
    @DeleteMapping("/{workoutId}/exercises/{exerciseId}")
    public ResponseEntity<Void> deleteExercise(
            Authentication authentication,
            @PathVariable UUID workoutId,
            @PathVariable UUID exerciseId) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        Workout workout = workoutService.getWorkoutById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        if (!workout.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Workout not found");
        }

        workoutService.removeExerciseFromWorkout(workoutId, exerciseId);

        return ResponseEntity.noContent().build();
    }

    /**
     * ENDPOINT 9: Add set entry to exercise
     * 
     * POST /api/workouts/{workoutId}/exercises/{exerciseId}/sets
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Finds exercise within workout
     * 2. Adds new set entry (reps + weight) to that exercise
     * 3. Returns 201 Created with updated WorkoutResponse (full data)
     * 
     * Use case: During workout, user finishes a set and logs it in real-time
     * Can add sets one at a time without updating entire workout
     * 
     * Path parameters:
     * - workoutId: UUID of workout
     * - exerciseId: UUID of exercise within that workout
     * 
     * Request body:
     * {
     * "reps": 8,
     * "weight": 80
     * }
     */
    @PostMapping("/{workoutId}/exercises/{exerciseId}/sets")
    public ResponseEntity<WorkoutResponse> addSetEntry(
            Authentication authentication,
            @PathVariable UUID workoutId,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody SetEntryRequest request) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        Workout workout = workoutService.getWorkoutById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        if (!workout.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Workout not found");
        }

        workoutService.addSetEntryToExercise(workoutId, exerciseId, request.reps(), request.weight());

        Workout updatedWorkout = workoutService.getWorkoutById(workoutId)
                .orElseThrow();

        WorkoutResponse response = workoutToResponse(updatedWorkout);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ENDPOINT 10: Update set entry (reps and/or weight)
     * 
     * PATCH /api/workouts/{workoutId}/exercises/{exerciseId}/sets/{setId}
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Finds set entry within exercise within workout
     * 2. Updates reps and/or weight (all fields optional)
     * 3. Returns 200 OK with updated WorkoutResponse
     * 
     * Use case: User recorded wrong reps/weight for a set, can update it
     * 
     * Path parameters:
     * - workoutId: UUID of workout
     * - exerciseId: UUID of exercise within that workout
     * - setId: UUID of set to update
     * 
     * Request body (all optional):
     * {
     * "reps": 10,
     * "weight": 75.5
     * }
     */
    @PatchMapping("/{workoutId}/exercises/{exerciseId}/sets/{setId}")
    public ResponseEntity<WorkoutResponse> updateSetEntry(
            Authentication authentication,
            @PathVariable UUID workoutId,
            @PathVariable UUID exerciseId,
            @PathVariable UUID setId,
            @Valid @RequestBody SetEntryRequest request) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        Workout workout = workoutService.getWorkoutById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        if (!workout.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Workout not found");
        }

        workoutService.updateSetEntry(workoutId, exerciseId, setId, request.reps(), request.weight());

        Workout updatedWorkout = workoutService.getWorkoutById(workoutId)
                .orElseThrow();

        WorkoutResponse response = workoutToResponse(updatedWorkout);
        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT 11: Delete set entry from exercise
     * 
     * DELETE /api/workouts/{workoutId}/exercises/{exerciseId}/sets/{setId}
     * Requires: JWT authentication
     * 
     * What it does:
     * 1. Finds set within exercise within workout
     * 2. Removes set entry
     * 3. Returns 204 No Content
     * 
     * BUSINESS RULE: Cannot delete last set (exercise must have ≥1 set)
     * If user tries to delete last set, gets error "Exercise must contain at least
     * one set entry"
     * 
     * Path parameters:
     * - workoutId: UUID of workout
     * - exerciseId: UUID of exercise
     * - setId: UUID of set to remove
     */
    @DeleteMapping("/{workoutId}/exercises/{exerciseId}/sets/{setId}")
    public ResponseEntity<Void> deleteSetEntry(
            Authentication authentication,
            @PathVariable UUID workoutId,
            @PathVariable UUID exerciseId,
            @PathVariable UUID setId) {

        UUID userId = UUID.fromString(authentication.getPrincipal().toString());

        Workout workout = workoutService.getWorkoutById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        if (!workout.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Workout not found");
        }

        workoutService.removeSetEntry(workoutId, exerciseId, setId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Helper method: Convert Workout entity to WorkoutResponse DTO
     * 
     * Why separate method? DTO conversion logic is complex and repeated multiple
     * times
     * Keeps code DRY (Don't Repeat Yourself)
     * 
     * @param workout The domain entity
     * @return The DTO ready to send to client
     */
    private WorkoutResponse workoutToResponse(Workout workout) {
        List<ExerciseResponse> exerciseResponses = workout.getExercises().stream()
                .map(exercise -> new ExerciseResponse(
                        exercise.getId(),
                        exercise.getName(),
                        exercise.getCategory().toString(),
                        exercise.getSets().stream()
                                .map(set -> new SetEntryResponse(set.getId(), set.getReps(), (double) set.getWeight()))
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());

        return new WorkoutResponse(
                workout.getId(),
                workout.getUser().getId(),
                workout.getDate(),
                workout.getTitle(),
                workout.getNotes(),
                exerciseResponses);
    }
}
