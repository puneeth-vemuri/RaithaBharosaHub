package com.raithabharosahub.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.raithabharosahub.MainActivity
import com.raithabharosahub.worker.WeatherRefreshWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val seasonDao: com.raithabharosahub.data.local.dao.SeasonDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val workManager: WorkManager by lazy(LazyThreadSafetyMode.NONE) {
        WorkManager.getInstance(context)
    }

    companion object {
        private val PREF_LANGUAGE = stringPreferencesKey("pref_language")
        private val PREF_NOTIFICATIONS = booleanPreferencesKey("pref_notifications")
        private val PREF_UNIT_SYSTEM = stringPreferencesKey("pref_unit_system")

        private const val DEFAULT_LANGUAGE = "en"
        private const val DEFAULT_UNIT_SYSTEM = "metric"
        private const val DEFAULT_NOTIFICATIONS = true
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            dataStore.data.map { preferences ->
                SettingsUiState(
                    selectedLanguage = preferences[PREF_LANGUAGE] ?: DEFAULT_LANGUAGE,
                    notificationsEnabled = preferences[PREF_NOTIFICATIONS] ?: DEFAULT_NOTIFICATIONS,
                    unitSystem = preferences[PREF_UNIT_SYSTEM] ?: DEFAULT_UNIT_SYSTEM,
                    appVersion = "1.0"
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PREF_LANGUAGE] = code
            }
            _uiState.value = _uiState.value.copy(selectedLanguage = code)

            val restartIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(restartIntent)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PREF_NOTIFICATIONS] = enabled
            }

            if (enabled) {
                scheduleWeatherRefreshWork()
            } else {
                workManager.cancelAllWorkByTag(WeatherRefreshWorker.WORKER_TAG)
            }

            _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        }
    }

    fun setUnitSystem(unit: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PREF_UNIT_SYSTEM] = unit
            }
            _uiState.value = _uiState.value.copy(unitSystem = unit)
        }
    }

    private fun scheduleWeatherRefreshWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
            30,
            java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WeatherRefreshWorker.WORKER_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WeatherRefreshWorker.WORKER_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    suspend fun getAllSeasonsStatic(): List<com.raithabharosahub.data.local.entity.SeasonEntity> {
        return seasonDao.getAll().first()
    }
}

data class SettingsUiState(
    val selectedLanguage: String = "en",
    val notificationsEnabled: Boolean = true,
    val unitSystem: String = "metric",
    val appVersion: String = "1.0"
)