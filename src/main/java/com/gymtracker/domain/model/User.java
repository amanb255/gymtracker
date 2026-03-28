package com.gymtracker.domain.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String hashedPassword;

    @Column(nullable = false)
    private Integer heightCm;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Workout> workouts;

    protected User() {
        this.workouts = new ArrayList<Workout>();
    }

    public User(String name, String email, String hashedPassword, LocalDate dateOfBirth, Integer heightCm, Gender gender) {
        this.id = UUID.randomUUID();
        this.workouts = new ArrayList<Workout>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Hashed password cannot be empty");
        }
        if (heightCm == null || heightCm <= 0) {
            throw new IllegalArgumentException("Height must be positive");
        }
        if (dateOfBirth == null || dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Invalid Date of Birth");
        }
        if (gender == null) {
            throw new IllegalArgumentException("Gender cannot be null");
        }

        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.heightCm = heightCm;
        this.name = name;
        this.email = email;
        this.hashedPassword = hashedPassword;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Workout> getWorkouts() {
        return new ArrayList<>(workouts);
    }

    public void updateName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.name = newName;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateHeight(Integer newHeightCm) {
        if (newHeightCm == null || newHeightCm <= 0) {
            throw new IllegalArgumentException("Height must be positive");
        }
        this.heightCm = newHeightCm;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateGender(Gender newGender) {
        if (newGender == null) {
            throw new IllegalArgumentException("Gender cannot be null");
        }
        this.gender = newGender;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePassword(String newHashedPassword) {
        if (newHashedPassword == null || newHashedPassword.isBlank()) {
            throw new IllegalArgumentException("Hashed password cannot be empty");
        }
        this.hashedPassword = newHashedPassword;
        this.updatedAt = LocalDateTime.now();
    }
}
