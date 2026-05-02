package com.raithabharosahub.data.generator

import com.raithabharosahub.data.local.entity.FarmerEntity
import com.raithabharosahub.data.local.entity.NpkEntity
import com.raithabharosahub.data.local.entity.PlotEntity
import com.raithabharosahub.data.local.entity.SeasonEntity
import com.raithabharosahub.data.local.entity.WeatherEntity
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Hilt-injected singleton that generates agronomically realistic sample data.
 *
 * Used by [com.raithabharosahub.data.repository.WeatherRepository] as the
 * last-resort fallback in the three-tier data chain:
 *   1. Retrofit API  →  2. assets/mock_weather.json  →  3. generateSimulatedWeather()
 *
 * All random values are bounded to ranges that make sense for agricultural
 * decision-making in the Indian sub-continental climate context.
 */
@Singleton
class DataGeneratorClass @Inject constructor() {

    private val random = Random.Default

    // ------------------------------------------------------------------
    // Primitive random helpers  (also called by ViewModels / calculators)
    // ------------------------------------------------------------------

    /**
     * Random soil moisture percentage.
     * Range: 10–40 % (typical field moisture suitable for sowing decisions).
     */
    fun randomMoisture(): Float {
        val mean = 22f
        val stdDev = 6f
        return (java.util.Random().nextGaussian() * stdDev + mean)
            .toFloat()
            .coerceIn(10f, 40f)
    }

    /**
     * Random air temperature in °C.
     * Range: 18–35 °C (ideal sowing window for kharif / rabi crops in India).
     */
    fun randomTemperature(): Float =
        (random.nextFloat() * (35f - 18f) + 18f)

    /**
     * Random relative humidity percentage.
     * Range: 40–90 % (representative of monsoon and post-monsoon periods).
     */
    fun randomHumidity(): Float =
        (random.nextFloat() * (90f - 40f) + 40f)

    /**
     * Random probability of precipitation, 0.0–1.0 scale.
     * 1.0 = 100% chance of rain.
     */
    fun randomRainProbability(): Float = random.nextFloat()

    // ------------------------------------------------------------------
    // Compound weather generator  (third-tier fallback)
    // ------------------------------------------------------------------

    /**
     * Generates a full 7-day, 3-hourly simulated forecast as a list of
     * 56 [WeatherEntity] rows for [plotId].
     *
     * Slot cadence mirrors the OpenWeatherMap /forecast response:
     *   7 days × 8 slots/day = 56 entries, each 3 hours apart.
     *
     * Realism notes:
     *  - Temperature follows a diurnal curve: cooler at night (00/03/06 UTC offsets),
     *    warmer mid-day (09/12/15 UTC offsets).
     *  - Rain is generated for ~30 % of slots (matches a typical monsoon period).
     *  - Humidity is inversely correlated with temperature (higher at night).
     *  - `lastUpdatedAt` and `fetchedAt` are both set to the moment of generation
     *    so the UI "Last updated" label correctly shows "just now".
     *
     * @param plotId  FK linking generated rows to a specific plot.
     * @return List of 56 [WeatherEntity] in chronological order.
     */
    fun generateSimulatedWeather(plotId: Long): List<WeatherEntity> {
        val now = Date()
        val threeHoursMs = 3L * 60L * 60L * 1_000L
        // Start from the beginning of today (truncate to day boundary, then back 3 days
        // so the forecast window is [-3 days … +4 days] which straddles "now" naturally).
        val startMs = roundToDay(System.currentTimeMillis()) - 3L * 24L * 60L * 60L * 1_000L

        // Base temperature with a gentle upward trend across the week (+0–3 °C day-over-day).
        val baseTempC = randomTemperature()

        return (0 until 56).map { slotIndex ->
            val slotMs   = startMs + slotIndex * threeHoursMs
            val slotDate = Date(slotMs)

            // Slot within the day (0–7): used to model diurnal temperature swing.
            val slotInDay = slotIndex % 8

            // Diurnal temperature offset: -3 °C at night slots (0,1,2,7) → +3 °C at peak (4,5).
            val diurnalOffset = diurnalTemperatureOffset(slotInDay)

            // Day-level temperature drift: +0.0–0.4 °C per day for a warming trend.
            val dayIndex   = slotIndex / 8
            val dayDrift   = dayIndex * (random.nextFloat() * 0.4f)

            val tempMax    = (baseTempC + diurnalOffset + dayDrift).coerceIn(18f, 35f)

            // Humidity inversely tracks temperature: hotter → drier (within range).
            val humidity   = (randomHumidity() - diurnalOffset * 1.5f).coerceIn(40f, 90f)

            // Rain: ~30 % of slots have precipitation; zero otherwise.
            val hasRain    = random.nextFloat() < 0.30f
            val rainMm     = if (hasRain) random.nextFloat() * 12f else 0f   // 0–12 mm per 3-hour slot

            WeatherEntity(
                id            = 0L,          // Room auto-generates
                plotId        = plotId,
                date          = slotDate,
                tempMax       = tempMax,
                humidity      = humidity,
                rainMm        = rainMm,
                fetchedAt     = now,
                lastUpdatedAt = now
            )
        }
    }

    // ------------------------------------------------------------------
    // Stub generators for other entity types  (implemented in later steps)
    // ------------------------------------------------------------------

    /** Generate sample farmers for development. */
    fun generateSampleFarmers(): List<FarmerEntity> =
        emptyList() // TODO: Implement in later step

    /** Generate sample plots for a given farmer. */
    fun generateSamplePlots(farmerId: Long): List<PlotEntity> =
        emptyList() // TODO: Implement in later step

    /**
     * Alias kept for call-sites that use the old name.
     * Delegates to [generateSimulatedWeather] so both names work.
     */
    fun generateSampleWeather(plotId: Long): List<WeatherEntity> =
        generateSimulatedWeather(plotId)

    /** Generate sample NPK readings for a plot. */
    fun generateSampleNpk(plotId: Long): List<NpkEntity> =
        emptyList() // TODO: Implement in later step

    /** Generate sample seasons for a plot. */
    fun generateSampleSeasons(plotId: Long): List<SeasonEntity> =
        emptyList() // TODO: Implement in later step

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Diurnal temperature offset (°C) for a given 3-hour slot within a day.
     * Slot 0 = 00:00 UTC, slot 4 = 12:00 UTC (peak heat), slot 7 = 21:00 UTC.
     */
    private fun diurnalTemperatureOffset(slotInDay: Int): Float = when (slotInDay) {
        0    -> -2.5f   // midnight
        1    -> -3.0f   // 03:00 — coolest
        2    -> -2.0f   // 06:00 — pre-dawn
        3    ->  0.5f   // 09:00 — warming
        4    ->  3.0f   // 12:00 — peak
        5    ->  2.5f   // 15:00 — still hot
        6    ->  1.0f   // 18:00 — cooling
        else -> -1.0f   // 21:00 — evening
    }

    /** Round epoch ms down to midnight of the current day (UTC). */
    private fun roundToDay(epochMs: Long): Long {
        val dayMs = 24L * 60L * 60L * 1_000L
        return (epochMs / dayMs) * dayMs
    }
}
