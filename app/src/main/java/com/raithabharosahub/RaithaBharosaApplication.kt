package com.raithabharosahub

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.raithabharosahub.worker.WeatherRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class for Raitha-Bharosa Hub.
 * Required for Hilt dependency injection.
 */
@HiltAndroidApp
class RaithaBharosaApplication : Application() {

    @Inject
    lateinit var workManager: WorkManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch(Dispatchers.Default) {
            scheduleWeatherRefresh()
        }
    }

    private fun scheduleWeatherRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
            30,
            TimeUnit.MINUTES
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
}