package com.raithabharosahub.presentation.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raithabharosahub.data.local.dao.FarmerDao
import com.raithabharosahub.data.local.dao.PlotDao
import com.raithabharosahub.data.local.entity.FarmerEntity
import com.raithabharosahub.data.local.entity.PlotEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 0,
    val language: String = "kn",
    val farmerName: String = "",
    val mobile: String = "",
    val primaryCrop: String = "",
    val district: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val plotLabel: String = "",
    val isComplete: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val farmerDao: FarmerDao,
    private val plotDao: PlotDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val preferences = dataStore.data.first()
            val language = preferences[LANGUAGE_KEY] ?: "kn"
            val isComplete = preferences[ONBOARDING_COMPLETE_KEY] ?: false
            _uiState.update {
                it.copy(language = language, isComplete = isComplete, isLoading = false)
            }
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[LANGUAGE_KEY] = language
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
            _uiState.update {
                it.copy(language = language, currentStep = 1)
            }
        }
    }

    fun saveProfile(
        farmerName: String,
        mobile: String,
        primaryCrop: String,
        district: String
    ) {
        _uiState.update {
            it.copy(
                farmerName = farmerName.trim(),
                mobile = mobile.trim(),
                primaryCrop = primaryCrop,
                district = district,
                currentStep = 2
            )
        }
    }

    fun savePlotPin(latitude: String, longitude: String, plotLabel: String) {
        _uiState.update {
            it.copy(
                latitude = latitude.trim(),
                longitude = longitude.trim(),
                plotLabel = plotLabel.trim(),
                currentStep = 3
            )
        }
    }

    fun markOnboardingDone(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val farmerId = farmerDao.insert(
                FarmerEntity(
                    name = state.farmerName,
                    mobile = state.mobile,
                    primaryCrop = state.primaryCrop,
                    district = state.district,
                    languagePref = state.language
                )
            )

            val lat = state.latitude.toDoubleOrNull() ?: 0.0
            val lon = state.longitude.toDoubleOrNull() ?: 0.0

            plotDao.insert(
                PlotEntity(
                    farmerId = farmerId,
                    latitude = lat,
                    longitude = lon,
                    label = state.plotLabel
                )
            )

            dataStore.edit { prefs ->
                prefs[ONBOARDING_COMPLETE_KEY] = true
            }

            _uiState.update {
                it.copy(isComplete = true, currentStep = 3)
            }
            onCompleted()
        }
    }

    companion object {
        // Use same preference key as SettingsViewModel / MainActivity
        val LANGUAGE_KEY = stringPreferencesKey("pref_language")
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
    }
}
