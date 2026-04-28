package com.raithabharosahub

import android.app.Application
import android.content.Context
import androidx.work.*
import com.raithabharosahub.worker.WeatherRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

/**
 * Application class for Raitha-Bharosa Hub.
 * Required for Hilt dependency injection.
 */
@HiltAndroidApp
class RaithaBharosaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleWeatherRefreshWork()
    }

    private fun scheduleWeatherRefreshWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
            6, // Repeat interval
            TimeUnit.HOURS,
            15, // Flex interval (15 minutes)
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, // Initial backoff delay
                TimeUnit.MINUTES
            )
            .addTag(WeatherRefreshWorker.WORKER_TAG)
            .build()

        val workManager = WorkManager.getInstance(this)
        
        // Use unique work to ensure only one weather refresh worker is scheduled
        workManager.enqueueUniquePeriodicWork(
            "weather_refresh_unique_work",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing schedule if already scheduled
            periodicWorkRequest
        )
    }
}