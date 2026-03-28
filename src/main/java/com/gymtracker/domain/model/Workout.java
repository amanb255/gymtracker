package com.gymtracker.domain.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workouts")
public class Workout {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Exercise> exercises;

    protected Workout() {
        this.exercises = new ArrayList<>();
    }

    public Workout(User user, LocalDate date, String title, String notes, List<Exercise> exercises) {
        if (user == null) {
            throw new IllegalArgumentException("Workout must belong to a user");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (exercises == null || exercises.isEmpty()) {
            throw new IllegalArgumentException("Workout must contain at least one exercise");
        }

        this.id = UUID.randomUUID();
        this.user = user;
        this.date = date;
        this.title = title;
        this.notes = notes;
        this.exercises = new ArrayList<>();

        for (Exercise exercise : exercises) {
            addExercise(exercise);
        }
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public User getUser() {
        return user;
    }

    public List<Exercise> getExercises() {
        return Collections.unmodifiableList(exercises);
    }

    public void addExercise(Exercise exercise) {
        if (exercise == null) {
            throw new IllegalArgumentException("Exercise cannot be null");
        }
        if (exercise.getWorkout() != null && exercise.getWorkout() != this) {
            throw new IllegalArgumentException("Exercise already assigned to another workout");
        }
        if (exercises.stream().anyMatch(e -> e.getName().equals(exercise.getName()))) {
            throw new IllegalArgumentException("Workout already contains an exercise with this name");
        }

        exercise.setWorkout(this);
        if (!exercises.contains(exercise)) {
            this.exercises.add(exercise);
        }
    }

    public void removeExercise(Exercise exercise) {
        if (exercise == null) {
            throw new IllegalArgumentException("Exercise cannot be null");
        }

        if (exercises.remove(exercise)) {
            exercise.clearWorkout();
        }

        if (exercises.isEmpty()) {
            throw new IllegalStateException("Workout must contain at least one exercise");
        }
    }

    public void updateTitle(String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        this.title = newTitle;
    }

    public void updateNotes(String newNotes) {
        this.notes = newNotes;
    }
}
