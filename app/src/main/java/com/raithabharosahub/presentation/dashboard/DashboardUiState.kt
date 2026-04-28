package com.raithabharosahub.presentation.dashboard

import com.raithabharosahub.data.local.entity.WeatherEntity
import com.raithabharosahub.domain.model.SowingResult

/**
 * UI state for the Dashboard screen.
 * Contains all data needed to render the dashboard: sowing index, weather metrics,
 * loading state, and error handling.
 */
data class DashboardUiState(
    val sowingResult: SowingResult? = null,
    val weatherList: List<WeatherEntity> = emptyList(),
    val moisture: Float = 0f,
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val rainForecast24h: Float = 0f,
    val isLoading: Boolean = false,
    val isOfflineMode: Boolean = false,
    val lastUpdated: String = "",
    val errorMessage: String? = null
) {
    companion object {
        val Empty = DashboardUiState()
    }
}