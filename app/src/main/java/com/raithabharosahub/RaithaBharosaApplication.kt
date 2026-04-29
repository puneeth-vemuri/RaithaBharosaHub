package com.raithabharosahub

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Raitha-Bharosa Hub.
 * Required for Hilt dependency injection.
 */
@HiltAndroidApp
class RaithaBharosaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // WorkManager scheduling is driven from SettingsViewModel to avoid
        // creating a second DataStore instance for the same preferences file.
    }
}