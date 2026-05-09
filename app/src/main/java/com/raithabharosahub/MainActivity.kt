package com.raithabharosahub

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.raithabharosahub.presentation.navigation.AppNavGraph
import com.raithabharosahub.ui.theme.RaithaBharosaHubTheme
import com.raithabharosahub.worker.WeatherRefreshWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    /**
     * WorkManager injected via Hilt (provided by AppModule.provideWorkManager).
     * Used to fire the expedited one-time fetch on first launch so the user
     * sees up-to-date weather data immediately, bypassing the Doze window.
     */
    @Inject
    lateinit var workManager: WorkManager

    private val PREF_LANGUAGE = stringPreferencesKey("pref_language")
    private val PREF_NOTIFICATIONS = booleanPreferencesKey("pref_notifications")
    private val DEFAULT_LANGUAGE = "en"
    private val DEFAULT_NOTIFICATIONS = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Set content immediately to show UI
        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                // handle results if needed
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }

            RaithaBharosaHubTheme {
                AppNavGraph()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val prefs = dataStore.data.first()

            // Restore locale preference
            val language = prefs[PREF_LANGUAGE] ?: DEFAULT_LANGUAGE
            withContext(Dispatchers.Main) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
            }

            // Enqueue an expedited one-time WorkRequest so the very first weather
            // fetch bypasses Doze mode and runs immediately, only if notifications are enabled.
            // ExistingWorkPolicy.KEEP (inside the helper) prevents duplicate runs
            // if the activity is recreated.
            val notificationsEnabled = prefs[PREF_NOTIFICATIONS] ?: DEFAULT_NOTIFICATIONS
            if (notificationsEnabled) {
                WeatherRefreshWorker.enqueueExpeditedFirstRun(workManager)
            }
        }
    }
}