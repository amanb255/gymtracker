package com.gymtracker.domain.repository;

import com.gymtracker.domain.model.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

    // Find all workouts for a specific user
    List<Workout> findByUserId(UUID userId);

    // Find workouts for a user within a date range
    List<Workout> findByUserIdAndDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    // Find workouts for a user on a specific date
    List<Workout> findByUserIdAndDate(UUID userId, LocalDate date);

    // Find the most recent workout for a user
    @Query("SELECT w FROM Workout w WHERE w.user.id = :userId ORDER BY w.date DESC")
    List<Workout> findTopByUserIdOrderByDateDesc(@Param("userId") UUID userId);

    // TODO: Future features/ideas:
    // - Add workout templates (predefined exercises)
    // - Implement workout progress tracking (compare sets over time)
    // - Add workout statistics (total volume, personal records)
    // - Support for workout sharing between users
    // - Integration with nutrition tracking
    // - Mobile app push notifications for workout reminders
    // - AI-powered workout recommendations based on goals
    // - Social features (leaderboards, challenges)
    // - Export workouts to PDF/CSV
    // - Barcode scanning for exercise logging
}