package com.raithabharosahub.domain.model

/**
 * Pure Kotlin data class representing NPK soil test entry.
 * Used for NPK recommendation calculations.
 */
data class NpkEntry(
    val nitrogen: Float,
    val phosphorus: Float,
    val potassium: Float,
    val testDate: Long, // timestamp
    val labName: String,
    val plotId: Long,
    val crop: String
)