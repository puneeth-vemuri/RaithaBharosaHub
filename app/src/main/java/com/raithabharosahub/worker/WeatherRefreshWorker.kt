package com.raithabharosahub.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.raithabharosahub.data.local.dao.PlotDao
import com.raithabharosahub.data.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val TAG = "WeatherRefreshWorker"

/**
 * Periodic background worker that refreshes weather data for all plots.
 * Scheduled to run every 6 hours with exponential backoff on failure.
 * Uses HiltWorker for dependency injection.
 */
@HiltWorker
class WeatherRefreshWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepository: WeatherRepository,
    private val plotDao: PlotDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "WeatherRefreshWorker started")

            // Get all plots from database - use first() terminal operator on Flow
            val plots = try {
                plotDao.getAll().first()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch plots from database", e)
                emptyList()
            }
            
            if (plots.isEmpty()) {
                Log.w(TAG, "No plots found in database, skipping weather refresh")
                return@withContext Result.success()
            }

            Log.d(TAG, "Found ${plots.size} plot(s) to refresh weather data")

            var successCount = 0
            var failureCount = 0

            // Refresh weather for each plot
            for (plot in plots) {
                try {
                    weatherRepository.refreshWeather(
                        plotId = plot.id,
                        latitude = plot.latitude,
                        longitude = plot.longitude
                    )
                    successCount++
                    Log.d(TAG, "Successfully refreshed weather for plot ${plot.id} (${plot.label})")
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Failed to refresh weather for plot ${plot.id} (${plot.label})", e)
                }
            }

            Log.d(TAG, "Weather refresh completed: $successCount succeeded, $failureCount failed")

            if (failureCount == plots.size) {
                // All attempts failed
                Result.failure()
            } else if (failureCount > 0) {
                // Partial success - retry with exponential backoff for failed plots
                Result.retry()
            } else {
                // All succeeded
                Result.success()
            }

        } catch (e: Exception) {
            Log.e(TAG, "WeatherRefreshWorker failed with unexpected error", e)
            Result.failure()
        }
    }

    companion object {
        /**
         * Creates input data for the worker (optional parameters).
         * Currently no input data needed.
         */
        fun createInputData(): Data = Data.Builder().build()

        /**
         * Worker tag for identification in WorkManager.
         */
        const val WORKER_TAG = "weather_refresh"
    }
}