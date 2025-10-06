package com.trailguide.android.data.dto

import com.google.gson.annotations.SerializedName

/**
 * OpenWeather API response DTOs.
 */
data class WeatherResponseDto(
    @SerializedName("main") val main: MainDto,
    @SerializedName("weather") val weather: List<WeatherDescriptionDto>,
    @SerializedName("wind") val wind: WindDto
)

data class MainDto(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("humidity") val humidity: Int
)

data class WeatherDescriptionDto(
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)

data class WindDto(
    @SerializedName("speed") val speed: Double
)

/**
 * Forecast API response.
 */
data class ForecastResponseDto(
    @SerializedName("list") val list: List<ForecastItemDto>
)

data class ForecastItemDto(
    @SerializedName("dt_txt") val dateTime: String,
    @SerializedName("main") val main: MainDto,
    @SerializedName("weather") val weather: List<WeatherDescriptionDto>,
    @SerializedName("pop") val precipitationProbability: Double
)


