package com.raithabharosahub.domain.generator

import com.raithabharosahub.data.local.entity.WeatherEntity
import java.util.Date
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates sample data for development and testing.
 * Provides a minimal realistic implementation for `generateSampleWeather` so
 * the Dashboard can display simulated data during development.
 */
@Singleton
class DataGeneratorClass @Inject constructor() {
    private val random = Random.Default

    /**
     * Generate sample farmers for development.
     */
    fun generateSampleFarmers(): List<com.raithabharosahub.data.local.entity.FarmerEntity> {
        return emptyList() // TODO: Implement in Step 3
    }

    /**
     * Generate sample plots for a given farmer.
     */
    fun generateSamplePlots(farmerId: Long): List<com.raithabharosahub.data.local.entity.PlotEntity> {
        return emptyList() // TODO: Implement in Step 3
    }

    /**
     * Generate sample weather data for a plot.
     * Returns a small list of `WeatherEntity` with varying values so the
     * DashboardViewModel can compute a sowing index and update UI state.
     */
    fun generateSampleWeather(plotId: Long): List<WeatherEntity> {
        val now = Date()
        val dayMs = 24L * 60L * 60L * 1000L

        val nowMs = System.currentTimeMillis()
        val variant = ((nowMs / 1000L) % 3L).toInt()

        return (0 until 3).map { i ->
            val date = Date(now.time - ((2 - i) * dayMs)) // past two days -> today
            val fetchedAt = Date()
            // Generate soil moisture percentage (10–40%). For the latest entry (i==2)
            // choose one of three variants (low/medium/high) based on current time so
            // repeated taps cycle the sowing state (GREEN/YELLOW/RED) predictably.
            val humidity = if (i == 2) {
                when (variant) {
                    0 -> 15f  // likely GREEN
                    1 -> 28f  // likely YELLOW
                    else -> 39f // likely RED (soil too wet threshold >38)
                }
            } else {
                (random.nextFloat() * (40f - 10f) + 10f).coerceIn(0f, 100f)
            }
            val tempMax = (random.nextFloat() * (35f - 18f) + 18f).coerceIn(-10f, 60f)
            val rainMm = (random.nextFloat() * 20f).coerceAtLeast(0f)

            WeatherEntity(
                id = 0L,
                plotId = plotId,
                date = date,
                rainMm = rainMm,
                tempMax = tempMax,
                humidity = humidity,
                fetchedAt = fetchedAt
            )
        }
    }

    /**
     * Generate sample NPK data for a plot.
     */
    fun generateSampleNpk(plotId: Long): List<com.raithabharosahub.data.local.entity.NpkEntity> {
        return emptyList() // TODO: Implement in Step 3
    }

    /**
     * Generate sample seasons for a plot.
     */
    fun generateSampleSeasons(plotId: Long): List<com.raithabharosahub.data.local.entity.SeasonEntity> {
        return emptyList() // TODO: Implement in Step 3
    }

    /**
     * Calculate sowing index based on weather and NPK data.
     */
    fun calculateSowingIndex(
        weatherData: com.raithabharosahub.data.local.entity.WeatherEntity,
        npkData: com.raithabharosahub.data.local.entity.NpkEntity
    ): Double {
        return 0.0 // TODO: Implement in Step 3
    }
}