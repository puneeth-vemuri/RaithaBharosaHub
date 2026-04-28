package com.raithabharosahub.domain.usecase

import com.raithabharosahub.data.local.entity.WeatherEntity
import com.raithabharosahub.data.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: Get weather forecast data for a plot.
 *
 * Thin wrapper around WeatherRepository.
 * No business logic here — just delegation to repository.
 * Repository decides whether to fetch from API or mock fallback.
 *
 * UI/ViewModel observes the Flow returned by this use case.
 */
@Singleton
class GetWeatherForecastUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {

    /**
     * Returns a Flow<List<WeatherEntity>> for the given plot.
     * UI observes this Flow and updates whenever data changes.
     *
     * @param plotId ID of the plot
     * @return Flow emitting weather entities
     */
    operator fun invoke(plotId: Long): Flow<List<WeatherEntity>> {
        return weatherRepository.getWeatherForecast(plotId)
    }
}
