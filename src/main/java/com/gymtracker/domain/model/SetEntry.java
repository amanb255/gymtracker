package com.gymtracker.domain.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "set_entries")
public class SetEntry {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private int reps;

    @Column(nullable = false)
    private double weight;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    protected SetEntry() {
    }

    public SetEntry(int reps, double weight) {
        if (reps <= 0) {
            throw new IllegalArgumentException("Reps must be greater than 0");
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }

        this.id = UUID.randomUUID();
        this.reps = reps;
        this.weight = weight;
    }

    public UUID getId() {
        return id;
    }

    public int getReps() {
        return reps;
    }

    public double getWeight() {
        return weight;
    }

    public Exercise getExercise() {
        return exercise;
    }

    void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    void clearExercise() {
        this.exercise = null;
    }

    public void updateReps(int reps) {
        if (reps <= 0) {
            throw new IllegalArgumentException("Reps must be greater than 0");
        }
        this.reps = reps;
    }

    public void updateWeight(double weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
        this.weight = weight;
    }
}
