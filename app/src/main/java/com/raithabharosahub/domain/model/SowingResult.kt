package com.raithabharosahub.domain.model

/**
 * Data class representing the result of a sowing index calculation.
 * Contains the numeric score, the state classification, and a localized message resource ID.
 *
 * @param score Normalized sowing index score (0–100)
 * @param state Current sowing condition state (GREEN, YELLOW, RED)
 * @param messageId Android string resource ID (e.g. R.string.sow_now) for UI display
 */
data class SowingResult(
    val score: Float,
    val state: SowingState,
    val messageId: Int
)
