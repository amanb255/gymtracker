package com.gymtracker.presentation.dto;

/**
 * WorkoutUpdateRequest - sent by client when updating an existing workout
 * 
 * All fields are optional (can update just title, just notes, or both)
 * Note: Cannot update date (would be confusing) or exercises (use separate
 * endpoints)
 * 
 * Fields:
 * - title: New workout title (optional, null/blank means no change)
 * - notes: New workout notes (optional, can be empty string, null means no
 * change)
 * 
 * Example: Update just the title
 * {
 * "title": "Updated Chest Day",
 * "notes": null
 * }
 */
public record WorkoutUpdateRequest(
        String title,
        String notes) {
}
