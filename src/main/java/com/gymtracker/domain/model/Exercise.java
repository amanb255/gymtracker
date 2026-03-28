package com.gymtracker.domain.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseCategory category;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SetEntry> sets;

    protected Exercise() {
        this.sets = new ArrayList<>();
    }

    public Exercise(String name, ExerciseCategory category, List<SetEntry> sets) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Exercise name cannot be empty");
        }
        if (category == null) {
            throw new IllegalArgumentException("Exercise category cannot be null");
        }
        if (sets == null || sets.isEmpty()) {
            throw new IllegalArgumentException("Exercise must contain at least one set entry");
        }

        this.id = UUID.randomUUID();
        this.name = name;
        this.category = category;
        this.sets = new ArrayList<>();

        for (SetEntry setEntry : sets) {
            addSetEntry(setEntry);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ExerciseCategory getCategory() {
        return category;
    }

    public Workout getWorkout() {
        return workout;
    }

    public List<SetEntry> getSets() {
        return Collections.unmodifiableList(sets);
    }

    void setWorkout(Workout workout) {
        this.workout = workout;
    }

    void clearWorkout() {
        this.workout = null;
    }

    public void addSetEntry(SetEntry setEntry) {
        if (setEntry == null) {
            throw new IllegalArgumentException("SetEntry cannot be null");
        }
        if (setEntry.getExercise() != null && setEntry.getExercise() != this) {
            throw new IllegalArgumentException("SetEntry already assigned to another exercise");
        }

        setEntry.setExercise(this);
        if (!sets.contains(setEntry)) {
            sets.add(setEntry);
        }
    }

    public void removeSetEntry(SetEntry setEntry) {
        if (setEntry == null) {
            throw new IllegalArgumentException("SetEntry cannot be null");
        }

        if (sets.remove(setEntry)) {
            setEntry.clearExercise();
        }

        if (sets.isEmpty()) {
            throw new IllegalStateException("Exercise must contain at least one set entry");
        }
    }

    public void updateName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Exercise name cannot be empty");
        }
        this.name = newName;
    }

    public void updateCategory(ExerciseCategory newCategory) {
        if (newCategory == null) {
            throw new IllegalArgumentException("Exercise category cannot be null");
        }
        this.category = newCategory;
    }
}
