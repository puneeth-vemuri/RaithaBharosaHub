package com.raithabharosahub.domain.model

/**
 * Represents a single day in the 7-day Krishi Calendar.
 * Pure Kotlin data class with no Android dependencies.
 */
data class KrishiDay(
    val date: Long, // timestamp in milliseconds
    val dayLabel: String, // "Mon", "Tue", etc.
    val weatherIcon: String, // emoji or icon code: "☀️", "🌧️", "⛅"
    val sowingScore: Float, // 0-100 sowing index
    val sowingState: SowingState, // SOW_NOW, CAUTION, WAIT, SOIL_TOO_WET
    val rainMm: Float, // precipitation in mm
    val hasStormWarning: Boolean, // true if next day has >15mm rain
    val stormWarningMessage: Int, // R.string.storm_warning_fertilize_today
    val recommendedAction: Int, // R.string.* resource ID for recommended action
    val cropMilestone: String? = null // nullable milestone text like "Sow Day"
)