package com.raithabharosahub

import android.app.Application
import androidx.work.WorkManager
import com.raithabharosahub.worker.WeatherRefreshWorker
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Raitha-Bharosa Hub.
 * Required for Hilt dependency injection.
 *
 * Scheduling strategy:
 *  - A PeriodicWorkRequest fires every 15 minutes (WorkManager minimum) with
 *    EXPONENTIAL backoff and a NETWORK_CONNECTED constraint.
 *  - WorkManager coalesces runs during Doze, so actual cadence is
 *    opportunistic — this keeps the battery target at ≤1%/hr.
 *  - The first launch also fires an expedited one-time request so the user
 *    sees fresh data immediately without waiting for the next periodic window.
 */
@HiltAndroidApp
class RaithaBharosaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val workManager = WorkManager.getInstance(this)
        // Enqueue the 15-min periodic job (idempotent — UPDATE policy deduplicates).
        WeatherRefreshWorker.enqueuePeriodicWork(workManager)
        // Note: The expedited first-run is enqueued from MainActivity.onCreate()
        // after the user's plot coordinates are available from DataStore / ViewModel.
    }
}