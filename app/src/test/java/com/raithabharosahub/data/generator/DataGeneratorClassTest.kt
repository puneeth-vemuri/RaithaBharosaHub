package com.raithabharosahub.data.generator

import org.junit.Test
import kotlin.test.assertTrue

/**
 * Unit tests for DataGeneratorClass.
 * Validates that all random generators produce values within expected ranges.
 */
class DataGeneratorClassTest {

    private val generator = DataGeneratorClass()

    /**
     * Test randomMoisture() always returns values in 10–40% range.
     * Run 1000 iterations to ensure consistent boundary behavior.
     */
    @Test
    fun testRandomMoisture_WithinRange() {
        val iterations = 1000
        repeat(iterations) {
            val moisture = generator.randomMoisture()
            assertTrue(
                moisture in 10f..40f,
                "Moisture $moisture should be in range 10–40%, iteration $it"
            )
        }
    }

    /**
     * Test randomTemperature() always returns values in 18–35°C range.
     * Run 1000 iterations to ensure consistent boundary behavior.
     */
    @Test
    fun testRandomTemperature_WithinRange() {
        val iterations = 1000
        repeat(iterations) {
            val temperature = generator.randomTemperature()
            assertTrue(
                temperature in 18f..35f,
                "Temperature $temperature should be in range 18–35°C, iteration $it"
            )
        }
    }

    /**
     * Test randomHumidity() always returns values in 40–90% range.
     */
    @Test
    fun testRandomHumidity_WithinRange() {
        val iterations = 1000
        repeat(iterations) {
            val humidity = generator.randomHumidity()
            assertTrue(
                humidity in 40f..90f,
                "Humidity $humidity should be in range 40–90%, iteration $it"
            )
        }
    }

    /**
     * Test randomRainProbability() always returns values in 0.0–1.0 range.
     */
    @Test
    fun testRandomRainProbability_WithinRange() {
        val iterations = 1000
        repeat(iterations) {
            val rainProb = generator.randomRainProbability()
            assertTrue(
                rainProb in 0f..1f,
                "Rain probability $rainProb should be in range 0.0–1.0, iteration $it"
            )
        }
    }

    /**
     * Test that randomMoisture() produces a variety of values (not constant).
     */
    @Test
    fun testRandomMoisture_VarietyOfValues() {
        val values = mutableSetOf<Float>()
        repeat(100) {
            values.add(generator.randomMoisture())
        }
        assertTrue(values.size > 50, "Should generate varied values, got ${values.size} unique")
    }

    /**
     * Test that randomTemperature() produces a variety of values (not constant).
     */
    @Test
    fun testRandomTemperature_VarietyOfValues() {
        val values = mutableSetOf<Float>()
        repeat(100) {
            values.add(generator.randomTemperature())
        }
        assertTrue(values.size > 50, "Should generate varied values, got ${values.size} unique")
    }
}
