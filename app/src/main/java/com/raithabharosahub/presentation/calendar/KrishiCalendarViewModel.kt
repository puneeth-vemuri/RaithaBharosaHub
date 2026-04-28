package com.raithabharosahub.presentation.calendar

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raithabharosahub.domain.model.KrishiDay
import com.raithabharosahub.domain.usecase.GetKrishiCalendarUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "calendar_prefs")

@HiltViewModel
class KrishiCalendarViewModel @Inject constructor(
    private val getKrishiCalendarUseCase: GetKrishiCalendarUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(KrishiCalendarUiState())
    val uiState: StateFlow<KrishiCalendarUiState> = _uiState.asStateFlow()

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

data class KrishiCalendarUiState(
    val days: List<KrishiDay> = emptyList(),
    val selectedDayIndex: Int = -1,
    val selectedDay: KrishiDay? = null,
    val isLoading: Boolean = false,
    val selectedPlotId: Long = 1L,
    val selectedCrop: String = "Paddy"
)