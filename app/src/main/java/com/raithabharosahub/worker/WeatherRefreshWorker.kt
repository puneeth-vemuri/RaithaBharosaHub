package com.raithabharosahub.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raithabharosahub.data.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "WeatherRefreshWorker"

/**
 * Periodic background worker that refreshes weather data for all plots.
 * Scheduled to run every 6 hours with exponential backoff on failure.
 * Uses HiltWorker for dependency injection.
 */
@HiltWorker
class WeatherRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val weatherRepository: WeatherRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "WeatherRefreshWorker started")

            val plotId = inputData.getLong(KEY_PLOT_ID, DEFAULT_PLOT_ID)
            val latitude = inputData.getDouble(KEY_LATITUDE, Double.NaN)
            val longitude = inputData.getDouble(KEY_LONGITUDE, Double.NaN)

            if (latitude.isNaN() || longitude.isNaN()) {
                Log.w(TAG, "WeatherRefreshWorker missing coordinates, skipping refresh")
                return@withContext Result.success()
            }

            weatherRepository.refreshWeather(plotId, latitude, longitude)
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "WeatherRefreshWorker failed with unexpected error", e)
            Result.failure()
        }
    }

    companion object {
        /**
         * Worker tag for identification in WorkManager.
         */
        const val WORKER_TAG = "weather_refresh"

        private const val DEFAULT_PLOT_ID = 1L
        private const val KEY_PLOT_ID = "plot_id"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
    }
}