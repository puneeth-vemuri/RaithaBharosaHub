package com.raithabharosahub.data.remote

import com.raithabharosahub.data.remote.dto.WeatherResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for OpenWeatherMap API.
 * Fetches 7-day 3-hourly weather forecast.
 */
interface WeatherApiService {

    /**
     * Fetch 7-day weather forecast (56 3-hourly items).
     *
     * @param latitude Latitude (decimal degrees)
     * @param longitude Longitude (decimal degrees)
     * @param apiKey OpenWeatherMap API key
     * @param units Temperature units ("metric" for Celsius)
     * @param cnt Number of forecast items to return (56 = 7 days × 8 per day)
     * @return WeatherResponseDto containing forecast list
     */
    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("cnt") cnt: Int = 56
    ): WeatherResponseDto
}