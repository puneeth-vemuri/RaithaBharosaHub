package com.raithabharosahub.presentation.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.raithabharosahub.R
import com.raithabharosahub.data.local.AppDatabase
import com.raithabharosahub.worker.WeatherRefreshWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val seasonDao: com.raithabharosahub.data.local.dao.SeasonDao,
    private val appDatabase: AppDatabase,
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

            // Modern per-app locale API — triggers UI recomposition without full restart
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(code)
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PREF_NOTIFICATIONS] = enabled
            }

            if (enabled) {
                // Delegate to the canonical factory in WeatherRefreshWorker —
                // 15-min interval, EXPONENTIAL backoff (15 min initial), CONNECTED constraint,
                // KEEP policy (no-op if already enqueued).
                WeatherRefreshWorker.enqueuePeriodicWork(workManager)
            } else {
                // Cancel only the unique periodic job — leave any in-flight expedited
                // first-run (enqueued by MainActivity) untouched.
                workManager.cancelUniqueWork(WeatherRefreshWorker.WORKER_TAG)
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

    fun exportSeasonHistory() {
        viewModelScope.launch {
            try {
                val seasons = seasonDao.getAll().first()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                // Build CSV string — columns: crop,sowDate,harvestDate,yieldKg,notes
                val csv = buildString {
                    appendLine("crop,sowDate,harvestDate,yieldKg,notes")
                    seasons.forEach { season ->
                        val crop       = season.crop.replace(",", ";")
                        val sowDate    = dateFormat.format(season.sowDate)
                        val harvestDate = season.harvestDate?.let { dateFormat.format(it) } ?: ""
                        val yieldKg    = season.yieldKg?.toString() ?: ""
                        val notes      = (season.notes ?: "").replace(",", ";").replace("\n", " ")
                        appendLine("$crop,$sowDate,$harvestDate,$yieldKg,$notes")
                    }
                }

                // Write to cacheDir/exports/ — must match file_paths.xml <cache-path path="exports/"/>
                val exportDir = File(context.cacheDir, "exports")
                withContext(Dispatchers.IO) {
                    exportDir.mkdirs()
                    val file = File(exportDir, "season_export.csv")
                    FileWriter(file).use { it.write(csv) }

                    val uri = FileProvider.getUriForFile(
                        context,
                        "com.raithabharosahub.fileprovider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export Season History").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }

                // Show success toast on main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.export_success_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.clearAllTables()
            withContext(Dispatchers.Main) {
                FirebaseAuth.getInstance().signOut()
                onSuccess()
            }
        }
    }
}

data class SettingsUiState(
    val selectedLanguage: String = "en",
    val notificationsEnabled: Boolean = true,
    val unitSystem: String = "metric",
    val appVersion: String = "1.0"
)