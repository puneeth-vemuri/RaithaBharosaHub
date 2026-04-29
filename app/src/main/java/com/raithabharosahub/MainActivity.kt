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
import com.raithabharosahub.presentation.navigation.AppNavGraph
import com.raithabharosahub.ui.theme.RaithaBharosaHubTheme
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

    private val PREF_LANGUAGE = stringPreferencesKey("pref_language")
    private val DEFAULT_LANGUAGE = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set content immediately to show UI
        setContent {
            RaithaBharosaHubTheme(dynamicColor = false) {
                AppNavGraph()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val language = dataStore.data.first()[PREF_LANGUAGE] ?: DEFAULT_LANGUAGE
            withContext(Dispatchers.Main) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
            }
        }
    }
}