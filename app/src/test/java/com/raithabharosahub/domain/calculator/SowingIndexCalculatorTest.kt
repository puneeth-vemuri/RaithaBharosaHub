package com.raithabharosahub.domain.calculator

import com.raithabharosahub.domain.model.SowingState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for SowingIndexCalculator.
 * Tests the weighted formula, thresholds, and crop-specific overrides.
 */
class SowingIndexCalculatorTest {

    private val calculator = SowingIndexCalculator()

    /**
     * Test GREEN band: ideal sowing conditions (score > 70)
     */
    @Test
    fun testGreenBand_IdealConditions() {
        val result = calculator.calculateSowingIndex(
            moisture = 28f,          // Optimal mid-range
            temperature = 26f,       // Optimal mid-range
            rainProbability = 0.1f   // Low rain probability
        )

        assertTrue(result.score > 70f, "Score should be > 70 for ideal conditions")
        assertEquals(SowingState.GREEN, result.state, "State should be GREEN")
    }

    /**
     * Test YELLOW band boundary: caution zone (40–70)
     */
    @Test
    fun testYellowBand_CautionZone() {
        val result = calculator.calculateSowingIndex(
            moisture = 20f,          // Lower end of acceptable
            temperature = 22f,       // Lower end of acceptable
            rainProbability = 0.5f   // Moderate rain
        )

        assertTrue(result.score in 40f..70f, "Score should be in YELLOW band (40–70)")
        assertEquals(SowingState.YELLOW, result.state, "State should be YELLOW")
    }

    /**
     * Test RED band: poor sowing conditions (score < 40)
     */
    @Test
    fun testRedBand_PoorConditions() {
        val result = calculator.calculateSowingIndex(
            moisture = 12f,          // Very low
            temperature = 18f,       // Minimum threshold
            rainProbability = 0.9f   // High rain probability
        )

        assertTrue(result.score < 40f, "Score should be < 40 for poor conditions")
        assertEquals(SowingState.RED, result.state, "State should be RED")
    }

    /**
     * Test soil_too_wet guard: force RED if moisture > 38%
     */
    @Test
    fun testSoilTooWet_Guard() {
        val result = calculator.calculateSowingIndex(
            moisture = 39f,          // Above 38% threshold
            temperature = 30f,       // Ideal temperature
            rainProbability = 0.0f   // No rain (normally ideal)
        )

        assertEquals(0f, result.score, "Score should be 0 when soil is too wet")
        assertEquals(SowingState.RED, result.state, "State should be RED")
        // Note: messageId should be MESSAGE_SOIL_TOO_WET (0x7F07_0004 placeholder)
    }

    /**
     * Test Paddy crop: moisture outside optimal range (25–35%) reduces score
     */
    @Test
    fun testPaddyCrop_MoistureOutsideOptimal() {
        // Ideal moisture for Paddy is 25–35%
        val resultOptimal = calculator.calculateSowingIndex(
            moisture = 30f,          // Within optimal (25–35%)
            temperature = 26f,
            rainProbability = 0.2f,
            crop = "Paddy"
        )

        // Non-optimal moisture (15% < 25%)
        val resultNonOptimal = calculator.calculateSowingIndex(
            moisture = 15f,          // Below optimal
            temperature = 26f,
            rainProbability = 0.2f,
            crop = "Paddy"
        )

        // Non-optimal score should be strictly lower than optimal
        // (both should be valid, but non-optimal is clamped by 20 points)
        assertTrue(resultNonOptimal.score < resultOptimal.score,
            "Non-optimal moisture should result in lower score")
    }

    /**
     * Test Ragi crop: moisture optimal range 20–30%
     */
    @Test
    fun testRagiCrop_MoistureOptimal() {
        // Moisture at 25% should be optimal for Ragi (within 20–30%)
        val resultOptimal = calculator.calculateSowingIndex(
            moisture = 25f,
            temperature = 26f,
            rainProbability = 0.2f,
            crop = "Ragi"
        )

        // Moisture at 35% is above optimal (>30%)
        val resultHigh = calculator.calculateSowingIndex(
            moisture = 35f,
            temperature = 26f,
            rainProbability = 0.2f,
            crop = "Ragi"
        )

        assertTrue(resultHigh.score < resultOptimal.score,
            "High moisture should result in lower score for Ragi")
    }

    /**
     * Test Sugarcane crop: moisture optimal range 22–32%
     */
    @Test
    fun testSugarcaneCrop_MoistureOptimal() {
        // Moisture at 27% should be optimal for Sugarcane (within 22–32%)
        val resultOptimal = calculator.calculateSowingIndex(
            moisture = 27f,
            temperature = 26f,
            rainProbability = 0.2f,
            crop = "Sugarcane"
        )

        // Moisture at 18% is below optimal (<22%)
        val resultLow = calculator.calculateSowingIndex(
            moisture = 18f,
            temperature = 26f,
            rainProbability = 0.2f,
            crop = "Sugarcane"
        )

        assertTrue(resultLow.score < resultOptimal.score,
            "Low moisture should result in lower score for Sugarcane")
    }

    /**
     * Test normalized score is between 0–100
     */
    @Test
    fun testScoreNormalization() {
        val result = calculator.calculateSowingIndex(
            moisture = 25f,
            temperature = 25f,
            rainProbability = 0.5f
        )

        assertTrue(result.score in 0f..100f, "Score must be normalized to 0–100")
    }

    /**
     * Test rain probability has inverse effect: high rain = lower score
     */
    @Test
    fun testRainProbabilityInverse() {
        val resultLowRain = calculator.calculateSowingIndex(
            moisture = 25f,
            temperature = 25f,
            rainProbability = 0.1f
        )

        val resultHighRain = calculator.calculateSowingIndex(
            moisture = 25f,
            temperature = 25f,
            rainProbability = 0.9f
        )

        assertTrue(resultLowRain.score > resultHighRain.score,
            "Low rain probability should result in higher score")
    }
}
