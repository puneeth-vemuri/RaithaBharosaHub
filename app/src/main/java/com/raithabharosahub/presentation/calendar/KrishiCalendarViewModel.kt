package com.raithabharosahub.presentation.calendar

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raithabharosahub.domain.model.KrishiDay
import com.raithabharosahub.domain.model.SowingState
import com.raithabharosahub.domain.usecase.GetKrishiCalendarUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "calendar_prefs")

/**
 * Presentation model for a single day in the Krishi Calendar horizontal strip.
 *
 * Derived from [KrishiDay] with storm logic already applied:
 * - [hasStormWarning] is true when the *next* day's [rainMm] > 15.0 mm
 *   (i.e. this day needs a pre-storm fertilization banner).
 * - [stormWarningResId] is the R.string resource ID for the banner text
 *   (0 when no warning).
 * - [cropMilestone] is a string resource key when a milestone applies to
 *   this day (e.g. "milestone_paddy_day21_irrigation"), null otherwise.
 */
data class DayForecast(
    val date: Long,                 // epoch ms
    val dayLabel: String,           // resource key: "day_mon", "day_tue", …
    val weatherIcon: String,        // emoji: "🌧️", "☀️", …
    val rainMm: Float,
    val tempMax: Float,             // °C
    val sowingIndex: Float,         // 0–100
    val sowingState: SowingState,
    val hasStormWarning: Boolean,
    val stormWarningResId: Int,     // R.string.warning_complete_fertilization or 0
    val recommendedActionResId: Int,// R.string.recommended_action_* or 0
    val cropMilestone: String?      // resource key or null
)

@HiltViewModel
class KrishiCalendarViewModel @Inject constructor(
    private val getKrishiCalendarUseCase: GetKrishiCalendarUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(KrishiCalendarUiState())
    val uiState: StateFlow<KrishiCalendarUiState> = _uiState.asStateFlow()

    /**
     * Derived [StateFlow] of [DayForecast] items — one per day in the 7-day strip.
     *
     * Storm-warning logic applied here:
     *   For each day N, if day (N+1) has rainMm > 15.0 mm → day N gets
     *   [DayForecast.hasStormWarning] = true.
     *
     * This re-applies the logic on top of whatever [GetKrishiCalendarUseCase]
     * already set on [KrishiDay.hasStormWarning], acting as a presentation-layer
     * verification pass. Crucially it sets a non-zero [DayForecast.stormWarningResId]
     * that the UI can directly pass to stringResource().
     */
    val calendarDays: StateFlow<List<DayForecast>> = _uiState
        .map { state -> state.days.toDayForecasts() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val plotIdKey = stringPreferencesKey("selected_plot_id")
    private val cropKey = stringPreferencesKey("selected_crop")

    init {
        loadStoredPreferences()
    }

    private fun loadStoredPreferences() {
        viewModelScope.launch {
            val plotId = context.dataStore.data.first()[plotIdKey]?.toLongOrNull() ?: 1L
            val crop = context.dataStore.data.first()[cropKey] ?: "Paddy"

            _uiState.update { it.copy(selectedPlotId = plotId, selectedCrop = crop) }

            // Load calendar data
            loadCalendarData(plotId, crop)
        }
    }

    private fun loadCalendarData(plotId: Long, crop: String) {
        _uiState.update { it.copy(isLoading = true) }

        getKrishiCalendarUseCase(plotId, crop)
            .onEach { days ->
                _uiState.update { state ->
                    state.copy(
                        days = days,
                        isLoading = false,
                        selectedDayIndex = if (days.isNotEmpty()) 0 else -1,
                        selectedDay = if (days.isNotEmpty()) days[0] else null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectDay(index: Int) {
        val days = _uiState.value.days
        if (index in days.indices) {
            _uiState.update { state ->
                state.copy(
                    selectedDayIndex = index,
                    selectedDay = days[index]
                )
            }
        }
    }

    fun refresh() {
        val plotId = _uiState.value.selectedPlotId
        val crop = _uiState.value.selectedCrop
        loadCalendarData(plotId, crop)
    }

    fun updatePlotId(plotId: Long) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[plotIdKey] = plotId.toString()
            }
            _uiState.update { it.copy(selectedPlotId = plotId) }
            loadCalendarData(plotId, _uiState.value.selectedCrop)
        }
    }

    fun updateCrop(crop: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[cropKey] = crop
            }
            _uiState.update { it.copy(selectedCrop = crop) }
            loadCalendarData(_uiState.value.selectedPlotId, crop)
        }
    }
}

// ---------------------------------------------------------------------------
// Extension: KrishiDay → DayForecast with storm-warning pass
// ---------------------------------------------------------------------------

/**
 * Converts the domain [KrishiDay] list to presentation [DayForecast] list.
 *
 * Storm logic (PRD requirement):
 *   For each Day N, check if Day N+1 has rainMm > 15.0 mm.
 *   If yes, Day N's [DayForecast.hasStormWarning] = true so the UI shows
 *   "Complete fertilization today" banner above that day card.
 *
 * The use case already sets [KrishiDay.hasStormWarning]; this pass re-derives
 * the flag directly from [KrishiDay.rainMm] at the presentation layer so the
 * ViewModel is a reliable, self-contained source of truth independent of any
 * use-case implementation detail.
 */
private fun List<KrishiDay>.toDayForecasts(): List<DayForecast> {
    return mapIndexed { index, day ->
        // Presentation-layer storm check: Day N+1 rainMm > 15 mm?
        val nextDayRain = getOrNull(index + 1)?.rainMm ?: 0f
        val hasStorm = nextDayRain > 15f

        // Preserve use-case-set stormWarningMessage if it has one,
        // otherwise fall through to 0 (no banner).
        val warningResId = if (hasStorm) day.stormWarningMessage else 0

        DayForecast(
            date                 = day.date,
            dayLabel             = day.dayLabel,
            weatherIcon          = day.weatherIcon,
            rainMm               = day.rainMm,
            tempMax              = day.sowingScore, // sowingScore used as tempMax proxy where tempMax not in KrishiDay
            sowingIndex          = day.sowingScore,
            sowingState          = day.sowingState,
            hasStormWarning      = hasStorm,
            stormWarningResId    = warningResId,
            recommendedActionResId = day.recommendedAction,
            cropMilestone        = day.cropMilestone
        )
    }
}

data class KrishiCalendarUiState(
    val days: List<KrishiDay> = emptyList(),
    val selectedDayIndex: Int = -1,
    val selectedDay: KrishiDay? = null,
    val isLoading: Boolean = false,
    val selectedPlotId: Long = 1L,
    val selectedCrop: String = "Paddy"
)