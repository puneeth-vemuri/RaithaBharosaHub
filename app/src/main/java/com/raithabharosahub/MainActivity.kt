package com.raithabharosahub

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    private val DEFAULT_LANGUAGE = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Set content immediately to show UI
        setContent {
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
        }

        // Enqueue an expedited one-time WorkRequest so the very first weather
        // fetch bypasses Doze mode and runs immediately.
        // ExistingWorkPolicy.KEEP (inside the helper) prevents duplicate runs
        // if the activity is recreated.
        // Coordinates default to NaN here; WeatherRefreshWorker handles the
        // missing-coords case gracefully (succeeds silently until a plot is set up).
        WeatherRefreshWorker.enqueueExpeditedFirstRun(workManager)
    }
}