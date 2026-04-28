package com.raithabharosahub.domain.model

/**
 * Enum representing the sowing condition state based on Sowing Index score.
 * Used to communicate readiness to farmers in UI and alerts.
 */
enum class SowingState {
    /**
     * GREEN: Excellent sowing conditions (score > 70)
     * Ready to sow immediately.
     */
    GREEN,

    /**
     * YELLOW: Caution zone (score 40–70)
     * Sowing is possible but conditions need monitoring.
     */
    YELLOW,

    /**
     * RED: Poor sowing conditions (score < 40)
     * Wait for better conditions before sowing.
     */
    RED
}
