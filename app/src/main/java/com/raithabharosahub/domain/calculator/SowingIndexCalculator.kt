package com.raithabharosahub.domain.calculator

import com.raithabharosahub.domain.model.SowingResult
import com.raithabharosahub.domain.model.SowingState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculates a weighted sowing index to determine crop planting readiness.
 *
 * Formula:
 *   rawScore = (moisture × 0.4) + (temperature × 0.3) + ((1 - rainProb) × 0.3)
 *
 * Normalisation to 0–100:
 *   - Moisture normalized to 0–1 (optimal 20–35% for most crops)
 *   - Temperature normalized to 0–1 (optimal 20–30°C)
 *   - Rain probability inverted (less rain is better for sowing)
 *
 * Thresholds:
 *   GREEN  (score > 70)  → "Sow Now" — ideal conditions
 *   YELLOW (40–70)       → "Caution" — monitor and check sub-conditions
 *   RED    (< 40)        → "Wait"   — poor conditions
 *
 * Crop-specific moisture overrides:
 *   Paddy      optimal: 25–35%
 *   Ragi       optimal: 20–30%
 *   Sugarcane  optimal: 22–32%
 *   If moisture is outside the optimal range, score is clamped down by 20 points.
 *
 * Guard condition:
 *   If moisture > 38%, force RED state ("Soil Too Wet").
 */
@Singleton
class SowingIndexCalculator @Inject constructor() {

    /**
     * String resource IDs for messages (will be injected at compile-time from R.string)
     * These are placeholder constants; at runtime they should use R.string.* values.
     */
    companion object {
        private const val MESSAGE_SOW_NOW = 0x7F07_0001          // Placeholder for R.string.sow_now
        private const val MESSAGE_CAUTION = 0x7F07_0002          // Placeholder for R.string.caution_check_conditions
        private const val MESSAGE_WAIT = 0x7F07_0003             // Placeholder for R.string.wait_conditions_poor
        private const val MESSAGE_SOIL_TOO_WET = 0x7F07_0004     // Placeholder for R.string.soil_too_wet
    }

    /**
     * Calculate sowing index based on weather and agronomic conditions.
     *
     * @param moisture Soil moisture percentage (0–100%)
     * @param temperature Air temperature in Celsius
     * @param rainProbability Probability of precipitation (0.0–1.0)
     * @param crop Optional crop type ("Paddy", "Ragi", "Sugarcane") for crop-specific overrides
     * @return SowingResult with score (0–100), state, and localized message ID
     */
    fun calculateSowingIndex(
        moisture: Float,
        temperature: Float,
        rainProbability: Float,
        crop: String? = null
    ): SowingResult {
        // Guard: If soil is too wet (>38%), force RED immediately
        if (moisture > 38f) {
            return SowingResult(
                score = 0f,
                state = SowingState.RED,
                messageId = MESSAGE_SOIL_TOO_WET
            )
        }

        // Normalize inputs to 0–1 scale for weighted calculation
        // Moisture: optimal range ~10–40%, so normalize 10–40 → 0–1
        val normalizedMoisture = (moisture - 10f) / (40f - 10f)
            .coerceIn(0f, 1f)

        // Temperature: optimal range ~18–35°C, so normalize 18–35 → 0–1
        val normalizedTemperature = (temperature - 18f) / (35f - 18f)
            .coerceIn(0f, 1f)

        // Rain probability: inverted (less rain = better for sowing)
        val inversePrecipitation = 1f - rainProbability

        // Weighted formula: moisture 40%, temperature 30%, inverse rain 30%
        var rawScore = (normalizedMoisture * 0.4f) +
                (normalizedTemperature * 0.3f) +
                (inversePrecipitation * 0.3f)

        // Apply crop-specific moisture override
        if (crop != null) {
            val (optimalMin, optimalMax) = getCropOptimalMoisture(crop)
            if (moisture < optimalMin || moisture > optimalMax) {
                // Clamp score down by 20 points if outside optimal range
                rawScore = (rawScore * 100f - 20f) / 100f
                    .coerceAtLeast(0f)
            }
        }

        // Normalize to 0–100 scale
        val score = (rawScore * 100f)
            .coerceIn(0f, 100f)

        // Determine state and message based on thresholds
        val (state, messageId) = when {
            score > 70f -> SowingState.GREEN to MESSAGE_SOW_NOW
            score >= 40f -> SowingState.YELLOW to MESSAGE_CAUTION
            else -> SowingState.RED to MESSAGE_WAIT
        }

        return SowingResult(
            score = score,
            state = state,
            messageId = messageId
        )
    }

    /**
     * Get crop-specific optimal moisture range.
     *
     * @param crop Crop name ("Paddy", "Ragi", "Sugarcane", etc.)
     * @return Pair of (minOptimal, maxOptimal) as percentage
     */
    private fun getCropOptimalMoisture(crop: String): Pair<Float, Float> {
        return when (crop) {
            "Paddy" -> 25f to 35f
            "Ragi" -> 20f to 30f
            "Sugarcane" -> 22f to 32f
            else -> 20f to 35f  // Default range
        }
    }

    /**
     * Convenience method to set message IDs from actual R.string resources.
     * Call this once during app initialization if injecting actual resource IDs is needed.
     *
     * For now, callers should override messageId mapping in a subclass or factory.
     */
    internal fun setMessageIds(
        sowNow: Int,
        caution: Int,
        wait: Int,
        soilTooWet: Int
    ) {
        // Note: Kotlin companion objects are immutable, so this is illustrative.
        // In production, use dependency injection to provide resource IDs.
    }
}
