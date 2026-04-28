package com.raithabharosahub.data.remote.dto

import com.squareup.moshi.Json

/**
 * Root response object from OpenWeatherMap forecast API.
 */
data class WeatherResponseDto(
    @Json(name = "list")
    val forecastList: List<ForecastDto>? = null,

    @Json(name = "city")
    val city: CityDto? = null
)

/**
 * Individual forecast item (3-hourly).
 */
data class ForecastDto(
    @Json(name = "dt")
    val dt: Long? = null,

    @Json(name = "dt_txt")
    val dtTxt: String? = null,

    @Json(name = "main")
    val main: MainDto? = null,

    @Json(name = "rain")
    val rain: RainDto? = null,

    @Json(name = "weather")
    val weather: List<WeatherConditionDto>? = null,

    @Json(name = "pop")
    val rainProbability: Float? = null
)

/**
 * Main weather metrics.
 */
data class MainDto(
    @Json(name = "temp")
    val temp: Float? = null,

    @Json(name = "temp_min")
    val tempMin: Float? = null,

    @Json(name = "temp_max")
    val tempMax: Float? = null,

    @Json(name = "humidity")
    val humidity: Int? = null,

    @Json(name = "pressure")
    val pressure: Int? = null
)

/**
 * Rainfall data (3-hour accumulation).
 */
data class RainDto(
    @Json(name = "3h")
    val threeHour: Float? = null
)

/**
 * Weather condition description.
 */
data class WeatherConditionDto(
    @Json(name = "id")
    val id: Int? = null,

    @Json(name = "main")
    val main: String? = null,

    @Json(name = "description")
    val description: String? = null,

    @Json(name = "icon")
    val icon: String? = null
)

/**
 * City metadata.
 */
data class CityDto(
    @Json(name = "id")
    val id: Long? = null,

    @Json(name = "name")
    val name: String? = null,

    @Json(name = "coord")
    val coord: CoordinateDto? = null,

    @Json(name = "country")
    val country: String? = null,

    @Json(name = "timezone")
    val timezone: Int? = null
)

/**
 * Geographic coordinates.
 */
data class CoordinateDto(
    @Json(name = "lat")
    val lat: Double? = null,

    @Json(name = "lon")
    val lon: Double? = null
)
