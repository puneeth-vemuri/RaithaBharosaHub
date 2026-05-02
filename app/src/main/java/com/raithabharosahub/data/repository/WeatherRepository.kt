package com.raithabharosahub.data.repository

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.raithabharosahub.data.generator.DataGeneratorClass
import com.raithabharosahub.data.local.dao.WeatherDao
import com.raithabharosahub.data.local.entity.WeatherEntity
import com.raithabharosahub.data.remote.WeatherApiService
import com.raithabharosahub.data.remote.dto.WeatherResponseDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WeatherRepository"

/**
 * Single source of truth for weather data.
 *
 * ## Three-tier data chain
 *
 * ```
 * refreshWeather(plotId, lat, lon)
 *  │
 *  ├─ 1. Retrofit API  ──────────────────► success → persist API response
 *  │       └─ IOException / HttpException
 *  │
 *  ├─ 2. assets/mock_weather.json ───────► success → persist mock response
 *  │       └─ any parse / IO exception
 *  │
 *  └─ 3. DataGeneratorClass ─────────────► generateSimulatedWeather(plotId)
 *          └─ always succeeds (pure computation)
 * ```
 *
 * The UI **never** talks to the network directly. It always reads from Room
 * via [getWeatherForecast], a cold Flow that Room keeps current after every write.
 *
 * ## API key
 * Injected by the OkHttp interceptor in NetworkModule via BuildConfig.OWM_API_KEY
 * (sourced from local.properties → never committed to VCS).
 */
@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val weatherApiService: WeatherApiService,
    private val weatherDao: WeatherDao,
    private val dataGenerator: DataGeneratorClass,
    private val moshi: Moshi
) {

    // ------------------------------------------------------------------
    // UI-facing read API (Room as single source of truth)
    // ------------------------------------------------------------------

    /**
     * Observe all forecast rows for [plotId] in chronological order.
     * Room emits a new list automatically whenever [refreshWeather] writes new data.
     */
    fun getWeatherForecast(plotId: Long): Flow<List<WeatherEntity>> {
        Log.d(TAG, "getWeatherForecast: subscribing for plotId=$plotId")
        return weatherDao.getByPlotId(plotId)
    }

    /**
     * One-shot read of the most recent refresh timestamp for [plotId].
     * Returns null if no data has ever been fetched.
     */
    suspend fun getLastUpdatedAt(plotId: Long): Date? =
        weatherDao.getLastUpdatedAt(plotId)

    /**
     * Reactive stream of the most recent refresh timestamp for [plotId].
     * Use this to drive a "Last updated X ago" UI label.
     */
    fun observeLastUpdatedAt(plotId: Long): Flow<Date?> =
        weatherDao.observeLastUpdatedAt(plotId)

    // ------------------------------------------------------------------
    // Write path — three-tier refresh
    // ------------------------------------------------------------------

    /**
     * Fetches fresh weather data via the three-tier chain and persists it to Room.
     *
     * Tier 1 — Retrofit (live network):
     *   Catches [IOException] (no connectivity, timeout, DNS) and [HttpException]
     *   (401 bad key, 429 rate-limit, 5xx server error) before falling to Tier 2.
     *
     * Tier 2 — assets/mock_weather.json (bundled fallback):
     *   Parses the bundled JSON with Moshi. If this also fails (corrupt asset, parse
     *   error), falls through to Tier 3.
     *
     * Tier 3 — [DataGeneratorClass.generateSimulatedWeather] (always succeeds):
     *   Pure computation — generates 56 realistic simulated forecast rows. This tier
     *   is guaranteed to succeed and therefore [WeatherRefreshWorker] will always
     *   return Result.success() rather than retry-looping when both network and asset
     *   are unavailable.
     *
     * @param plotId    FK linking saved rows to the target plot.
     * @param latitude  Decimal degrees latitude of the plot.
     * @param longitude Decimal degrees longitude of the plot.
     */
    suspend fun refreshWeather(plotId: Long, latitude: Double, longitude: Double) {
        Log.d(TAG, "refreshWeather START plotId=$plotId lat=$latitude lon=$longitude")
        val now = Date()

        // ----- Tier 1: Live Retrofit call -----
        val tier1Result = tryApiCall(latitude, longitude)

        if (tier1Result != null) {
            persistResponseDto(plotId = plotId, response = tier1Result, batchTimestamp = now)
            Log.i(TAG, "refreshWeather DONE via API for plotId=$plotId")
            return
        }

        // ----- Tier 2: assets/mock_weather.json -----
        val tier2Result = tryMockJson()

        if (tier2Result != null) {
            persistResponseDto(plotId = plotId, response = tier2Result, batchTimestamp = now)
            Log.i(TAG, "refreshWeather DONE via mock JSON for plotId=$plotId")
            return
        }

        // ----- Tier 3: DataGeneratorClass (pure computation — always succeeds) -----
        Log.w(TAG, "Both API and mock failed — using DataGenerator for plotId=$plotId")
        val generated = dataGenerator.generateSimulatedWeather(plotId)
        persistEntities(plotId = plotId, entities = generated)
        Log.i(TAG, "refreshWeather DONE via DataGenerator (${generated.size} rows) for plotId=$plotId")
    }

    // ------------------------------------------------------------------
    // Private: tier implementations
    // ------------------------------------------------------------------

    /**
     * Attempts a live Retrofit call.
     * Returns the parsed [WeatherResponseDto] on success, null on any network or HTTP error.
     */
    private suspend fun tryApiCall(latitude: Double, longitude: Double): WeatherResponseDto? {
        return try {
            val dto = weatherApiService.getForecast(
                latitude  = latitude,
                longitude = longitude
                // `appid`  injected by OkHttp interceptor in NetworkModule
                // `units`  defaults to "metric"
                // `cnt`    defaults to 56
            )
            Log.i(TAG, "Tier 1 SUCCESS — ${dto.forecastList?.size ?: 0} forecast items from API")
            dto
        } catch (e: IOException) {
            Log.w(TAG, "Tier 1 FAIL (IOException) — ${e.message}")
            null
        } catch (e: HttpException) {
            Log.w(TAG, "Tier 1 FAIL (HTTP ${e.code()}) — ${e.message()}")
            null
        }
    }

    /**
     * Attempts to read and parse assets/mock_weather.json.
     * Returns the parsed [WeatherResponseDto] on success, null if the asset is
     * missing, unreadable, or the JSON is malformed.
     */
    private fun tryMockJson(): WeatherResponseDto? {
        return try {
            val json = context.assets.open("mock_weather.json").use { stream ->
                InputStreamReader(stream, StandardCharsets.UTF_8).use { it.readText() }
            }
            val adapter  = moshi.adapter(WeatherResponseDto::class.java)
            val response = adapter.fromJson(json)
            if (response != null) {
                Log.i(TAG, "Tier 2 SUCCESS — mock_weather.json parsed (${json.length} chars)")
            } else {
                Log.w(TAG, "Tier 2 FAIL — Moshi returned null for mock_weather.json")
            }
            response
        } catch (e: Exception) {
            Log.w(TAG, "Tier 2 FAIL — could not load mock_weather.json: ${e.message}")
            null
        }
    }

    // ------------------------------------------------------------------
    // Private: persistence helpers
    // ------------------------------------------------------------------

    /**
     * Maps [WeatherResponseDto] → [WeatherEntity] list and persists via [persistEntities].
     *
     * Mapping rules:
     * | DTO field            | Entity field     | Notes                             |
     * |----------------------|------------------|-----------------------------------|
     * | `forecast.dt`        | `date`           | Unix seconds → Date (×1000)       |
     * | `main.temp_max`      | `tempMax`        | °C (metric)                       |
     * | `main.humidity`      | `humidity`       | 0–100 %                           |
     * | `rain.3h` (nullable) | `rainMm`         | 0f when absent / null             |
     * | `batchTimestamp`     | `lastUpdatedAt`  | Uniform across the whole batch    |
     * | `batchTimestamp`     | `fetchedAt`      | Same value for simplicity         |
     *
     * Slots where `dt` or `temp_max` are null are silently dropped.
     */
    private suspend fun persistResponseDto(
        plotId: Long,
        response: WeatherResponseDto,
        batchTimestamp: Date
    ) {
        val entities = response.forecastList?.mapNotNull { forecast ->
            try {
                val dt      = forecast.dt      ?: return@mapNotNull null.also {
                    Log.w(TAG, "Skipping forecast item: missing `dt`")
                }
                val tempMax = forecast.main?.tempMax ?: return@mapNotNull null.also {
                    Log.w(TAG, "Skipping dt=$dt: missing `main.temp_max`")
                }
                WeatherEntity(
                    id            = 0L,
                    plotId        = plotId,
                    date          = Date(dt * 1_000L),
                    tempMax       = tempMax,
                    humidity      = (forecast.main.humidity ?: 0).toFloat(),
                    rainMm        = forecast.rain?.threeHour ?: 0f,
                    fetchedAt     = batchTimestamp,
                    lastUpdatedAt = batchTimestamp
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error mapping forecast item — skipping", e)
                null
            }
        } ?: emptyList()

        persistEntities(plotId, entities)
    }

    /**
     * Atomically replaces all existing weather rows for [plotId] in Room:
     * delete stale data, then bulk-insert the new batch.
     *
     * The (plot_id, date) UNIQUE index with REPLACE conflict strategy handles
     * any remaining duplicates within the batch.
     */
    private suspend fun persistEntities(plotId: Long, entities: List<WeatherEntity>) {
        if (entities.isEmpty()) {
            Log.w(TAG, "persistEntities: nothing to save for plotId=$plotId")
            return
        }
        weatherDao.deleteByPlotId(plotId)
        weatherDao.insertAll(entities)
        Log.i(TAG, "Persisted ${entities.size} weather rows for plotId=$plotId")
    }
}
