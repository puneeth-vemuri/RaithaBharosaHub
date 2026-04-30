package com.raithabharosahub.data.repository

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.raithabharosahub.data.local.dao.WeatherDao
import com.raithabharosahub.data.local.entity.WeatherEntity
import com.raithabharosahub.domain.generator.DataGeneratorClass
import com.raithabharosahub.data.remote.WeatherApiService
import com.raithabharosahub.data.remote.dto.WeatherResponseDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WeatherRepository"

/**
 * Repository for weather data with API fallback to mock.
 * Single source of truth: Room database.
 * UI always reads from Room, never directly from API/DTO responses.
 */
@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val weatherApiService: WeatherApiService,
    private val weatherDao: WeatherDao,
    private val dataGenerator: DataGeneratorClass,
    private val moshi: Moshi
) {

    /**
     * Expose weather data as a Flow from Room.
     * UI observes this Flow and receives updates whenever data changes.
     */
    fun getWeatherForecast(plotId: Long): Flow<List<WeatherEntity>> {
        Log.d(TAG, "getWeatherForecast: Returning Flow for plotId=$plotId")
        return weatherDao.getByPlotId(plotId)
    }

    /**
     * Refresh weather data from API, with mock fallback.
     * 1. Try API call to OpenWeatherMap
     * 2. On any exception, load mock_weather.json from assets
     * 3. Parse JSON (Moshi)
     * 4. Map DTO to WeatherEntity and save to Room
     * 5. Flow from Room automatically notifies UI of changes
     */
    suspend fun refreshWeather(plotId: Long, latitude: Double, longitude: Double) {
        try {
            Log.d(TAG, "refreshWeather: Attempting API call for plotId=$plotId, lat=$latitude, lon=$longitude")
            
            // Step 1: Try API call
            val response = weatherApiService.getForecast(
                latitude = latitude,
                longitude = longitude,
                apiKey = ""  // API key injected by OkHttp interceptor; kept empty here
            )
            
            Log.d(TAG, "refreshWeather: API success, received ${response.forecastList?.size ?: 0} forecast items")
            
            // Step 4: Map DTO to WeatherEntity and save to Room
            saveWeatherData(plotId, response)
            
        } catch (e: Exception) {
            Log.w(TAG, "refreshWeather: API call failed, falling back to mock JSON", e)
            
            // Step 2: Load mock JSON from assets
            try {
                val mockJson = loadMockWeatherJson()
                Log.d(TAG, "refreshWeather: Successfully loaded mock_weather.json from assets")
                
                // Parse JSON with Moshi
                val adapter = moshi.adapter(WeatherResponseDto::class.java)
                val response = adapter.fromJson(mockJson)
                
                if (response != null) {
                    // Step 4: Map DTO to WeatherEntity and save to Room
                    saveWeatherData(plotId, response)
                    Log.d(TAG, "refreshWeather: Mock data saved to Room DB, plotId=$plotId")
                } else {
                    Log.e(TAG, "refreshWeather: Mock JSON parsed to null")
                    throw Exception("Mock JSON parsing resulted in null")
                }
                
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "refreshWeather: Even mock fallback failed", fallbackEx)
                throw fallbackEx
            }
        }
    }

    /**
     * Load mock_weather.json from assets.
     * Returns raw JSON string.
     */
    private fun loadMockWeatherJson(): String {
        return context.assets.open("mock_weather.json").use { inputStream ->
            InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                reader.readText()
            }
        }
    }

    /**
     * Map WeatherResponseDto to WeatherEntity list and save to Room.
     * DTO contains forecast list (56 items for 7 days).
     * Each forecast item becomes a WeatherEntity with:
     *   - plotId (from parameter)
     *   - date (converted from dt Unix timestamp)
     *   - temp, humidity, rainMm (from main and rain fields)
     *   - fetchedAt (current timestamp)
     *
     * Step 5: UI reads from Room via Flow — never directly from DTO
     */
    private suspend fun saveWeatherData(plotId: Long, response: WeatherResponseDto) {
        val entities = response.forecastList?.mapNotNull { forecast ->
            try {
                val tempMax = forecast.main?.tempMax ?: return@mapNotNull null
                val humidity = (forecast.main?.humidity ?: 0).toFloat()
                val rainMm = forecast.rain?.threeHour ?: 0f
                val dt = forecast.dt ?: return@mapNotNull null
                
                // Convert Unix timestamp (seconds) to Date
                val date = Date(dt * 1000L)
                
                WeatherEntity(
                    id = 0,  // Auto-generate by Room
                    plotId = plotId,
                    date = date,
                    rainMm = rainMm,
                    tempMax = tempMax,
                    humidity = humidity,
                    fetchedAt = Date()
                )
            } catch (e: Exception) {
                Log.w(TAG, "saveWeatherData: Failed to map forecast item", e)
                null
            }
        } ?: emptyList()
        
        if (entities.isNotEmpty()) {
            // Delete old data for this plot
            weatherDao.deleteByPlotId(plotId)
            // Insert new data
            weatherDao.insertAll(entities)
            Log.d(TAG, "saveWeatherData: Saved ${entities.size} weather entities for plotId=$plotId")
        } else {
            Log.w(TAG, "saveWeatherData: No valid forecast items to save")
        }
    }
}
