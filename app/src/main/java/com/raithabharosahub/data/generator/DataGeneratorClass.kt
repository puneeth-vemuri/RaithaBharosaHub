package com.raithabharosahub.data.generator

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Generates realistic sample weather data for development and testing.
 * All values are randomly generated within agronomically realistic ranges.
 * Uses kotlin.random.Random (no java.util.Random).
 */
@Singleton
class DataGeneratorClass @Inject constructor() {

    private val random = Random

    /**
     * Generate random soil moisture percentage.
     * Range: 10–40% (typical field moisture for sowing conditions)
     */
    fun randomMoisture(): Float {
        return random.nextFloat() * (40f - 10f) + 10f
    }

    /**
     * Generate random temperature in Celsius.
     * Range: 18–35°C (ideal sowing temperature window)
     */
    fun randomTemperature(): Float {
        return random.nextFloat() * (35f - 18f) + 18f
    }

    /**
     * Generate random relative humidity percentage.
     * Range: 40–90% (typical field humidity)
     */
    fun randomHumidity(): Float {
        return random.nextFloat() * (90f - 40f) + 40f
    }

    /**
     * Generate random probability of precipitation (0–1 scale).
     * Range: 0.0–1.0 (where 1.0 = 100% chance of rain)
     */
    fun randomRainProbability(): Float {
        return random.nextFloat()
    }

    /**
     * Generate sample farmers for development.
     */
    fun generateSampleFarmers(): List<com.raithabharosahub.data.local.entity.FarmerEntity> {
        return emptyList() // TODO: Implement in later step
    }

    /**
     * Generate sample plots for a given farmer.
     */
    fun generateSamplePlots(farmerId: Long): List<com.raithabharosahub.data.local.entity.PlotEntity> {
        return emptyList() // TODO: Implement in later step
    }

    /**
     * Generate sample weather data for a plot.
     */
    fun generateSampleWeather(plotId: Long): List<com.raithabharosahub.data.local.entity.WeatherEntity> {
        return emptyList() // TODO: Implement in later step
    }

    /**
     * Generate sample NPK data for a plot.
     */
    fun generateSampleNpk(plotId: Long): List<com.raithabharosahub.data.local.entity.NpkEntity> {
        return emptyList() // TODO: Implement in later step
    }

    /**
     * Generate sample seasons for a plot.
     */
    fun generateSampleSeasons(plotId: Long): List<com.raithabharosahub.data.local.entity.SeasonEntity> {
        return emptyList() // TODO: Implement in later step
    }
}
