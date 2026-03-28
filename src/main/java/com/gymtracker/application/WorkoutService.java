package com.gymtracker.application;

import com.gymtracker.domain.model.*;
import com.gymtracker.domain.repository.UserRepository;
import com.gymtracker.domain.repository.WorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;

    public WorkoutService(WorkoutRepository workoutRepository, UserRepository userRepository) {
        this.workoutRepository = workoutRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Workout> getAllWorkouts() {
        return workoutRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Workout> getWorkoutById(UUID id) {
        return workoutRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Workout> getWorkoutsForUser(UUID userId) {
        return workoutRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Workout> getWorkoutsForUserInDateRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        return workoutRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Workout> getWorkoutsForUserOnDate(UUID userId, LocalDate date) {
        return workoutRepository.findByUserIdAndDate(userId, date);
    }

    @Transactional(readOnly = true)
    public List<Workout> getRecentWorkoutsForUser(UUID userId) {
        return workoutRepository.findTopByUserIdOrderByDateDesc(userId);
    }

    public Workout createWorkout(UUID userId, LocalDate date, String title, String notes, List<Exercise> exercises) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Workout workout = new Workout(user, date, title, notes, exercises);
        return workoutRepository.save(workout);
    }

    public void addExerciseToWorkout(UUID workoutId, Exercise exercise) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        workout.addExercise(exercise);
        // No need to save explicitly - @Transactional will flush changes
    }

    public void removeExerciseFromWorkout(UUID workoutId, UUID exerciseId) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        Exercise exerciseToRemove = workout.getExercises().stream()
                .filter(e -> e.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found in workout"));

        workout.removeExercise(exerciseToRemove);
    }

    public Workout updateWorkout(UUID workoutId, String title, String notes) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        if (title != null && !title.isBlank()) {
            workout.updateTitle(title);
        }
        if (notes != null) {
            workout.updateNotes(notes);
        }

        return workoutRepository.save(workout);
    }

    public void deleteWorkout(UUID workoutId) {
        if (!workoutRepository.existsById(workoutId)) {
            throw new IllegalArgumentException("Workout not found");
        }
        workoutRepository.deleteById(workoutId);
    }

    /**
     * Update exercise name and/or category
     */
    public Exercise updateExercise(UUID workoutId, UUID exerciseId, String newName, String newCategory) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        Exercise exercise = workout.getExercises().stream()
                .filter(e -> e.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found in workout"));

        if (newName != null && !newName.isBlank()) {
            exercise.updateName(newName);
        }
        if (newCategory != null) {
            try {
                exercise.updateCategory(ExerciseCategory.valueOf(newCategory.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid exercise category");
            }
        }

        return workoutRepository.save(workout).getExercises().stream()
                .filter(e -> e.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Add a set entry to an exercise
     */
    public SetEntry addSetEntryToExercise(UUID workoutId, UUID exerciseId, int reps, double weight) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        Exercise exercise = workout.getExercises().stream()
                .filter(e -> e.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found in workout"));

        SetEntry setEntry = new SetEntry(reps, weight);
        exercise.addSetEntry(setEntry);
        workoutRepository.save(workout);

        return setEntry;
    }

    /**
     * Update a set entry (reps and/or weight)
     */
    public SetEntry updateSetEntry(UUID workoutId, UUID exerciseId, UUID setEntryId, Integer newReps,
            Double newWeight) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        Exercise exercise = workout.getExercises().stream()
                .filter(e -> e.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found in workout"));

        SetEntry setEntry = exercise.getSets().stream()
                .filter(s -> s.getId().equals(setEntryId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Set entry not found in exercise"));

        if (newReps != null) {
            setEntry.updateReps(newReps);
        }
        if (newWeight != null) {
            setEntry.updateWeight(newWeight);
        }

        workoutRepository.save(workout);
        return setEntry;
    }

    /**
     * Remove a set entry from an exercise
     */
    public void removeSetEntry(UUID workoutId, UUID exerciseId, UUID setEntryId) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));

        Exercise exercise = workout.getExercises().stream()
                .filter(e -> e.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found in workout"));

        SetEntry setEntry = exercise.getSets().stream()
                .filter(s -> s.getId().equals(setEntryId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Set entry not found in exercise"));

        exercise.removeSetEntry(setEntry);
        workoutRepository.save(workout);
    }

    // TODO: Future - add methods for workout statistics, progress tracking, etc.
}