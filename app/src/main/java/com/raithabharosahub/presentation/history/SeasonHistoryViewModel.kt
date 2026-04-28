package com.raithabharosahub.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raithabharosahub.data.local.dao.SeasonDao
import com.raithabharosahub.data.local.entity.SeasonEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * UI state for Season History screen.
 */
data class SeasonHistoryUiState(
    val seasons: List<SeasonEntity> = emptyList(),
    val selectedCropFilter: String? = null,
    val isAddingNew: Boolean = false,
    val formState: SeasonFormState = SeasonFormState(),
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Form state for adding/editing a season.
 */
data class SeasonFormState(
    val crop: String = "",
    val sowDate: String = "",
    val harvestDate: String = "",
    val yieldKg: String = "",
    val notes: String = "",
    val validationErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class SeasonHistoryViewModel @Inject constructor(
    private val seasonDao: SeasonDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeasonHistoryUiState())
    val uiState: StateFlow<SeasonHistoryUiState> = _uiState.asStateFlow()

    private val allSeasonsFlow = seasonDao.getAll()

    init {
        // Combine all seasons flow with crop filter
        combine(
            allSeasonsFlow,
            _uiState
        ) { seasons, state ->
            val filteredSeasons = if (state.selectedCropFilter != null) {
                seasons.filter { it.crop == state.selectedCropFilter }
            } else {
                seasons
            }
            state.copy(
                seasons = filteredSeasons,
                isLoading = false
            )
        }.onEach { newState ->
            _uiState.update { it.copy(seasons = newState.seasons, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    /**
     * Select a crop filter or clear it (null = show all).
     */
    fun selectFilter(crop: String?) {
        _uiState.update { it.copy(selectedCropFilter = crop) }
    }

    /**
     * Update a form field.
     */
    fun updateFormField(field: SeasonFormField, value: String) {
        _uiState.update { state ->
            val newFormState = when (field) {
                SeasonFormField.CROP -> state.formState.copy(crop = value)
                SeasonFormField.SOW_DATE -> state.formState.copy(sowDate = value)
                SeasonFormField.HARVEST_DATE -> state.formState.copy(harvestDate = value)
                SeasonFormField.YIELD_KG -> state.formState.copy(yieldKg = value)
                SeasonFormField.NOTES -> state.formState.copy(notes = value)
            }
            state.copy(formState = newFormState)
        }
    }

    /**
     * Toggle the "Add New" form visibility.
     */
    fun toggleAddNew() {
        _uiState.update { state ->
            state.copy(
                isAddingNew = !state.isAddingNew,
                formState = if (!state.isAddingNew) SeasonFormState() else state.formState,
                isSaved = false
            )
        }
    }

    /**
     * Save a new season entry.
     */
    fun saveEntry() {
        val state = _uiState.value
        val form = state.formState

        // Validate form
        val errors = mutableMapOf<String, String>()
        if (form.crop.isBlank()) {
            errors["crop"] = "Crop is required"
        }
        if (form.sowDate.isBlank()) {
            errors["sowDate"] = "Sowing date is required"
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(formState = form.copy(validationErrors = errors)) }
            return
        }

        viewModelScope.launch {
            try {
                // For simplicity, using current plotId = 1 (should come from DataStore)
                val plotId = 1L

                // Parse dates (simplified - in real app use DatePicker)
                val sowDate = try {
                    Date(form.sowDate.toLong())
                } catch (e: Exception) {
                    Date() // fallback to current date
                }

                val harvestDate = if (form.harvestDate.isNotBlank()) {
                    try {
                        Date(form.harvestDate.toLong())
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }

                val yieldKg = if (form.yieldKg.isNotBlank()) {
                    try {
                        form.yieldKg.toFloat()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }

                val seasonEntity = SeasonEntity(
                    plotId = plotId,
                    crop = form.crop,
                    sowDate = sowDate,
                    harvestDate = harvestDate,
                    yieldKg = yieldKg,
                    notes = form.notes.ifBlank { null }
                )

                seasonDao.insert(seasonEntity)

                // Reset form and show success
                _uiState.update {
                    it.copy(
                        isAddingNew = false,
                        formState = SeasonFormState(),
                        isSaved = true,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to save season: ${e.message}")
                }
            }
        }
    }

    /**
     * Delete a season entry.
     */
    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            try {
                seasonDao.deleteById(id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to delete season: ${e.message}")
                }
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * Enum for form fields.
 */
enum class SeasonFormField {
    CROP,
    SOW_DATE,
    HARVEST_DATE,
    YIELD_KG,
    NOTES
}