package com.raithabharosahub.domain.model

/**
 * Domain model representing a cropping season.
 * Uses Long timestamps (milliseconds since epoch) for dates.
 */
data class Season(
    val id: Long = 0,
    val plotId: Long,
    val crop: String,
    val sowDate: Long, // milliseconds since epoch
    val harvestDate: Long?, // nullable for ongoing seasons
    val yieldKg: Float?, // nullable if not harvested yet
    val notes: String? // optional notes
) {
    /**
     * Returns whether this season is completed (has harvest date and yield).
     */
    val isCompleted: Boolean
        get() = harvestDate != null && yieldKg != null

    /**
     * Returns the duration of the season in days.
     * If harvest date is not set, returns null.
     */
    val durationDays: Int?
        get() = harvestDate?.let { harvest ->
            val days = (harvest - sowDate) / (1000 * 60 * 60 * 24)
            days.toInt()
        }
}