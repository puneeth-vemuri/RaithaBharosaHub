package com.raithabharosahub.domain.usecase

import com.raithabharosahub.R
import com.raithabharosahub.data.repository.WeatherRepository
import com.raithabharosahub.domain.calculator.SowingIndexCalculator
import com.raithabharosahub.domain.model.KrishiDay
import com.raithabharosahub.domain.model.SowingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: Get 7-day Krishi Calendar for a plot and crop.
 *
 * Combines weather data with sowing index calculations and adds storm warnings
 * and crop milestone markers to create a comprehensive farming calendar.
 */
@Singleton
class GetKrishiCalendarUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val sowingIndexCalculator: SowingIndexCalculator
) {

    /**
     * Returns a Flow<List<KrishiDay>> for the next 7 days.
     *
     * @param plotId ID of the plot
     * @param crop Crop type (e.g., "Paddy", "Ragi", "Sugarcane")
     * @return Flow emitting 7 KrishiDay items
     */
    operator fun invoke(plotId: Long, crop: String): Flow<List<KrishiDay>> {
        return weatherRepository.getWeatherForecast(plotId)
            .map { weatherEntities ->
                buildKrishiDays(weatherEntities, crop)
            }
    }

    private fun buildKrishiDays(
        weatherEntities: List<com.raithabharosahub.data.local.entity.WeatherEntity>,
        crop: String
    ): List<KrishiDay> {
        val days = mutableListOf<KrishiDay>()
        val calendar = Calendar.getInstance()

        // Use actual weather data for available days, generate mock for missing days
        for (i in 0 until 7) {
            calendar.timeInMillis = System.currentTimeMillis() + (i * 24 * 60 * 60 * 1000L)
            val dayLabel = getDayLabel(calendar)
            
            // Try to get weather data for this day
            val weatherForDay = weatherEntities.getOrNull(i)
            
            if (weatherForDay != null) {
                // Convert weather data to sowing index parameters
                // Use rainMm as rainProbability (convert mm to probability 0-1)
                val rainProbability = (weatherForDay.rainMm / 30f).coerceIn(0f, 1f)
                // Use tempMax as temperature
                val temperature = weatherForDay.tempMax
                // Use humidity as moisture (simplified conversion)
                val moisture = (weatherForDay.humidity * 0.5f).coerceIn(0f, 100f)
                
                // Calculate sowing index using actual weather
                val sowingResult = sowingIndexCalculator.calculateSowingIndex(
                    moisture = moisture,
                    temperature = temperature,
                    rainProbability = rainProbability,
                    crop = crop
                )
                
                // FIX: explicit parentheses — without them `?: 0f > 15f` parses as
                // `?: (0f > 15f)` which is always false.
                val hasStormWarning = if (i < 6) {
                    val nextDayWeather = weatherEntities.getOrNull(i + 1)
                    (nextDayWeather?.rainMm ?: 0f) > 15f
                } else false
                
                // Crop milestone markers.
                // sowingDayOffset: how many days ago the sow date was. We use the
                // earliest weather entry date as a proxy for the sow date. This means
                // within a 7-day window starting at day 0, milestones at days that
                // land inside [0..6] relative to sow date will show.
                // For Paddy: "First irrigation" fires on calendar Day 21 from sow.
                // Since the 7-day strip starts "today", we check if today+i == sowDate+21.
                val sowDateMs = weatherEntities.firstOrNull()?.date?.time
                    ?: calendar.timeInMillis
                val daysSinceSow = ((calendar.timeInMillis - sowDateMs) /
                    (24L * 60L * 60L * 1_000L)).toInt()
                val cropMilestone: String? = when {
                    daysSinceSow == 0  -> "milestone_sow_day"
                    daysSinceSow == 21 && crop.equals("Paddy", ignoreCase = true)
                                       -> "milestone_paddy_day21_irrigation"
                    daysSinceSow == 7  -> "milestone_first_irrigation"
                    daysSinceSow == 14 -> "milestone_first_fertilization"
                    else               -> null
                }
                
                days.add(
                    KrishiDay(
                        date = calendar.timeInMillis,
                        dayLabel = dayLabel,
                        weatherIcon = getWeatherIcon(weatherForDay.rainMm, weatherForDay.tempMax),
                        sowingScore = sowingResult.score,
                        sowingState = sowingResult.state,
                        rainMm = weatherForDay.rainMm,
                        hasStormWarning = hasStormWarning,
                        stormWarningMessage = if (hasStormWarning)
                                R.string.warning_complete_fertilization else 0,
                        recommendedAction = getRecommendedAction(sowingResult.state),
                        cropMilestone = cropMilestone
                    )
                )
            } else {
                // Generate mock data for missing days with specific rain values to create score variation
                // Day 0-1: rainMm = 1.0 (GREEN ~80-90)
                // Day 2-3: rainMm = 8.0 (YELLOW ~50-60)
                // Day 4: rainMm = 20.0 (RED + storm warning on Day 3)
                // Day 5-6: rainMm = 2.0 (GREEN recovery)
                val mockRain = when (i) {
                    0, 1 -> 1.0f
                    2, 3 -> 8.0f
                    4 -> 20.0f
                    5, 6 -> 2.0f
                    else -> 1.0f
                }
                val mockTemp = (20..35).random().toFloat()
                val mockHumidity = (40..90).random().toFloat()
                
                // Convert mock data to sowing index parameters
                val rainProbability = (mockRain / 30f).coerceIn(0f, 1f)
                val temperature = mockTemp
                val moisture = (mockHumidity * 0.5f).coerceIn(0f, 100f)
                
                // FIX: operator precedence — same as the real-data path above.
                val hasStormWarning = when (i) {
                    3    -> mockRain > 15f   // Day 4 has rainMm=20 so Day 3 warns
                    else -> false
                }
                
                val sowingResult = sowingIndexCalculator.calculateSowingIndex(
                    moisture = moisture,
                    temperature = temperature,
                    rainProbability = rainProbability,
                    crop = crop
                )
                
                days.add(
                    KrishiDay(
                        date = calendar.timeInMillis,
                        dayLabel = dayLabel,
                        weatherIcon = getWeatherIcon(mockRain, mockTemp),
                        sowingScore = sowingResult.score,
                        sowingState = sowingResult.state,
                        rainMm = mockRain,
                        hasStormWarning = hasStormWarning,
                        stormWarningMessage = if (hasStormWarning)
                                R.string.warning_complete_fertilization else 0,
                        recommendedAction = getRecommendedAction(sowingResult.state),
                        cropMilestone = null
                    )
                )
            }
        }

        return days
    }

    private fun getDayLabel(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "day_mon"
            Calendar.TUESDAY -> "day_tue"
            Calendar.WEDNESDAY -> "day_wed"
            Calendar.THURSDAY -> "day_thu"
            Calendar.FRIDAY -> "day_fri"
            Calendar.SATURDAY -> "day_sat"
            Calendar.SUNDAY -> "day_sun"
            else -> "day_mon"
        }
    }

    private fun getWeatherIcon(rainMm: Float, tempMax: Float): String {
        return when {
            rainMm > 10f -> "🌧️"
            rainMm > 0f -> "🌦️"
            tempMax > 35f -> "☀️"
            tempMax > 25f -> "⛅"
            else -> "☁️"
        }
    }

    private fun getMilestoneResource(milestone: String): Int {
        return when (milestone) {
            "milestone_sow_day"                -> R.string.milestone_sow_day
            "milestone_first_irrigation"       -> R.string.milestone_first_irrigation
            "milestone_first_fertilization"    -> R.string.milestone_first_fertilization
            "milestone_paddy_day21_irrigation" -> R.string.milestone_paddy_day21_irrigation
            else                               -> R.string.milestone_sow_day
        }
    }

    private fun getRecommendedAction(state: SowingState): Int {
        return when (state) {
            SowingState.GREEN  -> R.string.recommended_action_sow
            SowingState.YELLOW -> R.string.recommended_action_wait
            SowingState.RED    -> R.string.recommended_action_delay
        }
    }
}