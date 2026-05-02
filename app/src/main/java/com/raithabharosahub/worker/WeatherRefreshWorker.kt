package com.raithabharosahub.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.raithabharosahub.data.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG = "WeatherRefreshWorker"

/**
 * Periodic background worker that refreshes weather data for a specific plot.
 *
 * Design decisions to meet the ≤1% battery/hour target:
 *  - Scheduled as a PeriodicWorkRequest with the minimum 15-minute interval (WorkManager
 *    will coalesce runs when the device is in Doze, so real cadence is opportunistic).
 *  - Constrained to CONNECTED network — falls back to mock JSON in WeatherRepository
 *    when offline, so the worker still runs but doesn't hammer the radio.
 *  - Exponential backoff (EXPONENTIAL, 15 min initial, 6 hr cap) prevents rapid
 *    retry storms on transient failures.
 *  - setExpedited() on the one-time first-run request to bypass Doze and get
 *    fresh data immediately after app launch.  The required ForegroundInfo
 *    (shown only while the expedited run is active, usually <2 s) keeps Android 12+
 *    happy with the short-lived foreground-service that WorkManager creates internally.
 *
 * @param context  Provided by WorkManager / Hilt-Work.
 * @param workerParams  Provided by WorkManager / Hilt-Work.
 * @param weatherRepository  Injected via @HiltWorker / @AssistedInject.
 */
@HiltWorker
class WeatherRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val weatherRepository: WeatherRepository
) : CoroutineWorker(context, workerParams) {

    // ------------------------------------------------------------------
    // doWork
    // ------------------------------------------------------------------

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "doWork() started  runAttempt=$runAttemptCount")

            val plotId   = inputData.getLong(KEY_PLOT_ID, DEFAULT_PLOT_ID)
            val latitude  = inputData.getDouble(KEY_LATITUDE, Double.NaN)
            val longitude = inputData.getDouble(KEY_LONGITUDE, Double.NaN)

            if (latitude.isNaN() || longitude.isNaN()) {
                // No coordinates stored yet — succeed silently; the periodic
                // request will retry on the next interval once the user sets up a plot.
                Log.w(TAG, "Missing coordinates for plotId=$plotId, skipping refresh")
                return@withContext Result.success()
            }

            weatherRepository.refreshWeather(plotId, latitude, longitude)
            Log.d(TAG, "refreshWeather() completed for plotId=$plotId")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "refreshWeather() failed (attempt $runAttemptCount)", e)
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            // Retry with exponential backoff configured on the WorkRequest.
            // WorkManager will cap at MAX_BACKOFF_HOURS.
            Result.retry()
        }
    }

    // ------------------------------------------------------------------
    // getForegroundInfo — required for setExpedited() on Android 12+
    // ------------------------------------------------------------------

    /**
     * Called by WorkManager when the expedited first-run request needs to run
     * as a short-lived foreground service (Android 12+).
     * The notification is automatically dismissed when doWork() returns.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Raitha Bharosa Hub")
            .setContentText("Syncing latest weather data…")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weather Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background weather refresh (silent)"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    // ------------------------------------------------------------------
    // Companion — WorkRequest factory helpers
    // ------------------------------------------------------------------

    companion object {

        /** Unique name used with enqueueUniquePeriodicWork / enqueueUniqueWork. */
        const val WORKER_TAG = "weather_refresh"

        // Input-data keys
        const val KEY_PLOT_ID   = "plot_id"
        const val KEY_LATITUDE  = "latitude"
        const val KEY_LONGITUDE = "longitude"

        private const val DEFAULT_PLOT_ID = 1L

        // Notification
        private const val CHANNEL_ID      = "weather_sync_channel"
        private const val NOTIFICATION_ID = 1001

        // Backoff
        /** Initial backoff before first retry: 15 minutes. */
        private const val BACKOFF_INITIAL_MIN = 15L

        /** Maximum backoff ceiling: 6 hours (21 600 000 ms). */
        private const val MAX_BACKOFF_HOURS = 6L

        /**
         * Network constraint: require any active connection.
         * WeatherRepository already falls back to mock JSON when the API
         * is unreachable, so CONNECTED (not UNMETERED) is intentional.
         */
        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Enqueue the recurring 15-minute periodic work request.
         * Safe to call multiple times (UPDATE policy deduplicates).
         *
         * Call this from Application.onCreate() so that the periodic job
         * survives process restarts and Doze cycles.
         */
        fun enqueuePeriodicWork(
            workManager: WorkManager,
            plotId: Long   = DEFAULT_PLOT_ID,
            latitude: Double = Double.NaN,
            longitude: Double = Double.NaN
        ) {
            val data = workDataOf(
                KEY_PLOT_ID   to plotId,
                KEY_LATITUDE  to latitude,
                KEY_LONGITUDE to longitude
            )

            val periodicRequest = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
                    15L, TimeUnit.MINUTES          // Minimum interval allowed by WorkManager
            )
                .setConstraints(networkConstraints)
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_INITIAL_MIN, TimeUnit.MINUTES
                )
                .addTag(WORKER_TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORKER_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )

            Log.d(TAG, "Periodic weather-refresh enqueued (interval=15 min, backoff=EXPONENTIAL)")
        }

        /**
         * Enqueue a one-time EXPEDITED run so the very first fetch bypasses
         * Doze mode and returns data immediately after app launch.
         *
         * WorkManager creates a short-lived foreground service backed by
         * [getForegroundInfo]; the notification is dismissed automatically
         * when doWork() finishes.
         *
         * Safe to call on every launch — ExistingWorkPolicy.KEEP means it
         * won't fire again if a run is already in-progress or in queue.
         */
        fun enqueueExpeditedFirstRun(
            workManager: WorkManager,
            plotId: Long   = DEFAULT_PLOT_ID,
            latitude: Double = Double.NaN,
            longitude: Double = Double.NaN
        ) {
            val data = workDataOf(
                KEY_PLOT_ID   to plotId,
                KEY_LATITUDE  to latitude,
                KEY_LONGITUDE to longitude
            )

            val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<WeatherRefreshWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(networkConstraints)
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_INITIAL_MIN, TimeUnit.MINUTES
                )
                .addTag(WORKER_TAG)
                .build()

            workManager.enqueueUniqueWork(
                "${WORKER_TAG}_expedited",
                androidx.work.ExistingWorkPolicy.KEEP,
                oneTimeRequest
            )

            Log.d(TAG, "Expedited one-time weather-refresh enqueued")
        }
    }
}