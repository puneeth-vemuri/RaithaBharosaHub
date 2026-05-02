package com.raithabharosahub.data.remote

import com.raithabharosahub.data.remote.dto.WeatherResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the OpenWeatherMap 5-day / 3-hour forecast API.
 *
 * BASE_URL: https://api.openweathermap.org/   (set in NetworkModule)
 * ENDPOINT: data/2.5/forecast
 *
 * API key injection:
 *   The `appid` query parameter is injected automatically by the
 *   OkHttp interceptor in NetworkModule (provideOpenWeatherApiKeyInterceptor).
 *   It is NOT declared here as a @Query param — that would duplicate the
 *   parameter and could conflict if the interceptor key differs.
 *
 * API key source:
 *   BuildConfig.OWM_API_KEY  ←  local.properties: OWM_API_KEY=<your_key>
 *   Never committed to source control.
 */
interface WeatherApiService {

    /**
     * Fetch a 7-day, 3-hourly weather forecast.
     *
     * @param latitude   Plot latitude in decimal degrees.
     * @param longitude  Plot longitude in decimal degrees.
     * @param units      "metric" → temperatures in °C, wind in m/s.
     * @param cnt        Number of 3-hour slots to return. 56 = 7 days × 8 slots/day.
     * @return [WeatherResponseDto] containing a `list` of [ForecastDto] items.
     */
    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat")   latitude:  Double,
        @Query("lon")   longitude: Double,
        @Query("units") units: String = "metric",
        @Query("cnt")   cnt: Int = 56
    ): WeatherResponseDto
}