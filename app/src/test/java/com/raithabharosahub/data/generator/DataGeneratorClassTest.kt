package com.raithabharosahub.data.generator

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM unit tests for [DataGeneratorClass].
 *
 * Strategy: run each random accessor 1000 times and assert every returned
 * value stays within the documented contract range.
 *
 * Ranges (from source):
 *   randomMoisture()    → 10f..40f
 *   randomTemperature() → 18f..35f
 *   randomHumidity()    → 40f..90f
 */
class DataGeneratorClassTest {

    private lateinit var generator: DataGeneratorClass

    @Before
    fun setUp() {
        generator = DataGeneratorClass()
    }

    // ------------------------------------------------------------------
    // randomMoisture()  contract: always in [10f, 40f]
    // ------------------------------------------------------------------

    @Test
    fun `randomMoisture always returns value in 10f to 40f over 1000 iterations`() {
        repeat(1_000) { i ->
            val value = generator.randomMoisture()
            assertTrue("Iteration $i: randomMoisture()=$value is below 10f", value >= 10f)
            assertTrue("Iteration $i: randomMoisture()=$value is above 40f", value <= 40f)
        }
    }

    @Test
    fun testRandomMoisture_VarietyOfValues() {
        val values = mutableSetOf<Float>()
        repeat(100) { values.add(generator.randomMoisture()) }
        assertTrue("Should generate varied values, got ${values.size} unique", values.size > 50)
    }

    // ------------------------------------------------------------------
    // randomTemperature()  contract: always in [18f, 35f]
    // ------------------------------------------------------------------

    @Test
    fun `randomTemperature always returns value in 18f to 35f over 1000 iterations`() {
        repeat(1_000) { i ->
            val value = generator.randomTemperature()
            assertTrue("Iteration $i: randomTemperature()=$value is below 18f", value >= 18f)
            assertTrue("Iteration $i: randomTemperature()=$value is above 35f", value <= 35f)
        }
    }

    @Test
    fun testRandomTemperature_VarietyOfValues() {
        val values = mutableSetOf<Float>()
        repeat(100) { values.add(generator.randomTemperature()) }
        assertTrue("Should generate varied values, got ${values.size} unique", values.size > 50)
    }

    // ------------------------------------------------------------------
    // randomHumidity()  contract: always in [40f, 90f]
    // ------------------------------------------------------------------

    @Test
    fun `randomHumidity always returns value in 40f to 90f over 1000 iterations`() {
        repeat(1_000) { i ->
            val value = generator.randomHumidity()
            assertTrue("Iteration $i: randomHumidity()=$value is below 40f", value >= 40f)
            assertTrue("Iteration $i: randomHumidity()=$value is above 90f", value <= 90f)
        }
    }

    // ------------------------------------------------------------------
    // randomRainProbability()  contract: always in [0f, 1f]
    // ------------------------------------------------------------------

    @Test
    fun testRandomRainProbability_WithinRange() {
        repeat(1_000) {
            val rainProb = generator.randomRainProbability()
            assertTrue(
                "Rain probability $rainProb should be in range 0.0–1.0, iteration $it",
                rainProb in 0f..1f
            )
        }
    }

    // ------------------------------------------------------------------
    // generateSimulatedWeather()  contract: 56 rows, valid fields
    // ------------------------------------------------------------------

    @Test
    fun `generateSimulatedWeather returns exactly 56 rows`() {
        val rows = generator.generateSimulatedWeather(plotId = 1L)
        assertEquals(
            "Expected 56 rows (7 days x 8 intervals), got ${rows.size}",
            56, rows.size
        )
    }

    @Test
    fun `generateSimulatedWeather all rows carry the correct plotId`() {
        val plotId = 99L
        generator.generateSimulatedWeather(plotId).forEachIndexed { i, entity ->
            assertEquals("Row $i: plotId must be $plotId", plotId, entity.plotId)
        }
    }

    @Test
    fun `generateSimulatedWeather rainMm is within sane range for all rows`() {
        generator.generateSimulatedWeather(1L).forEachIndexed { i, entity ->
            assertTrue("Row $i: rainMm=${entity.rainMm} must be >= 0", entity.rainMm >= 0f)
            assertTrue("Row $i: rainMm=${entity.rainMm} must be <= 100", entity.rainMm <= 100f)
        }
    }

    @Test
    fun `generateSimulatedWeather tempMax is within sane range for all rows`() {
        generator.generateSimulatedWeather(1L).forEachIndexed { i, entity ->
            assertTrue("Row $i: tempMax=${entity.tempMax} must be >= 18", entity.tempMax >= 18f)
            assertTrue("Row $i: tempMax=${entity.tempMax} must be <= 35", entity.tempMax <= 35f)
        }
    }

    @Test
    fun `generateSimulatedWeather humidity is within sane range for all rows`() {
        generator.generateSimulatedWeather(1L).forEachIndexed { i, entity ->
            assertTrue("Row $i: humidity=${entity.humidity} must be >= 40", entity.humidity >= 40f)
            assertTrue("Row $i: humidity=${entity.humidity} must be <= 90", entity.humidity <= 90f)
        }
    }

    @Test
    fun `generateSimulatedWeather dates are non-null and in non-decreasing order`() {
        val rows = generator.generateSimulatedWeather(1L)
        for (i in 1 until rows.size) {
            assertNotNull("Row $i: date must not be null", rows[i].date)
            assertTrue(
                "Row $i date=${rows[i].date.time} must be >= row ${i-1} date=${rows[i-1].date.time}",
                rows[i].date.time >= rows[i - 1].date.time
            )
        }
    }
}
