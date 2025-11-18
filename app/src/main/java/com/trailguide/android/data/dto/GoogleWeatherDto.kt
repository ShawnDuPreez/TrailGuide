package com.trailguide.android.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Google Weather API response DTOs.
 */

// Current Conditions DTOs
data class GoogleCurrentConditionsDto(
    @SerializedName("temperature") val temperature: TemperatureDto,
    @SerializedName("feelsLikeTemperature") val feelsLikeTemperature: TemperatureDto?,
    @SerializedName("relativeHumidity") val relativeHumidity: Int?,
    @SerializedName("weatherCondition") val weatherCondition: WeatherConditionDto,
    @SerializedName("precipitation") val precipitation: PrecipitationDto?,
    @SerializedName("timeZone") val timeZone: TimeZoneDto?
)

data class TemperatureDto(
    @SerializedName("degrees") val degrees: Double,
    @SerializedName("unit") val unit: String
)

data class WeatherConditionDto(
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: DescriptionDto,
    @SerializedName("iconBaseUri") val iconBaseUri: String?
)

data class DescriptionDto(
    @SerializedName("text") val text: String,
    @SerializedName("languageCode") val languageCode: String
)

data class PrecipitationDto(
    @SerializedName("probability") val probability: PrecipitationProbabilityDto?
)

data class PrecipitationProbabilityDto(
    @SerializedName("percent") val percent: Int?,
    @SerializedName("type") val type: String?
)

data class TimeZoneDto(
    @SerializedName("id") val id: String
)

// Forecast DTOs
data class GoogleForecastResponseDto(
    @SerializedName("forecastDays") val forecastDays: List<ForecastDayDto>,
    @SerializedName("timeZone") val timeZone: TimeZoneDto?,
    @SerializedName("nextPageToken") val nextPageToken: String?
)

data class ForecastDayDto(
    @SerializedName("interval") val interval: IntervalDto,
    @SerializedName("displayDate") val displayDate: DisplayDateDto,
    @SerializedName("daytimeForecast") val daytimeForecast: DaytimeForecastDto?,
    @SerializedName("nighttimeForecast") val nighttimeForecast: NighttimeForecastDto?,
    @SerializedName("maxTemperature") val maxTemperature: TemperatureDto,
    @SerializedName("minTemperature") val minTemperature: TemperatureDto,
    @SerializedName("feelsLikeMaxTemperature") val feelsLikeMaxTemperature: TemperatureDto?,
    @SerializedName("feelsLikeMinTemperature") val feelsLikeMinTemperature: TemperatureDto?
)

data class DaytimeForecastDto(
    @SerializedName("interval") val interval: IntervalDto,
    @SerializedName("weatherCondition") val weatherCondition: WeatherConditionDto,
    @SerializedName("relativeHumidity") val relativeHumidity: Int?,
    @SerializedName("uvIndex") val uvIndex: Int?,
    @SerializedName("precipitation") val precipitation: PrecipitationDto?,
    @SerializedName("thunderstormProbability") val thunderstormProbability: Int?,
    @SerializedName("wind") val wind: GoogleWindDto?,
    @SerializedName("cloudCover") val cloudCover: Int?
)

data class NighttimeForecastDto(
    @SerializedName("interval") val interval: IntervalDto,
    @SerializedName("weatherCondition") val weatherCondition: WeatherConditionDto,
    @SerializedName("relativeHumidity") val relativeHumidity: Int?,
    @SerializedName("uvIndex") val uvIndex: Int?,
    @SerializedName("precipitation") val precipitation: PrecipitationDto?,
    @SerializedName("thunderstormProbability") val thunderstormProbability: Int?,
    @SerializedName("wind") val wind: GoogleWindDto?,
    @SerializedName("cloudCover") val cloudCover: Int?
)

data class GoogleWindDto(
    @SerializedName("direction") val direction: WindDirectionDto?,
    @SerializedName("speed") val speed: SpeedDto?,
    @SerializedName("gust") val gust: SpeedDto?
)

data class WindDirectionDto(
    @SerializedName("degrees") val degrees: Int?,
    @SerializedName("cardinal") val cardinal: String?
)

data class SpeedDto(
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String
)

data class DisplayDateDto(
    @SerializedName("year") val year: Int,
    @SerializedName("month") val month: Int,
    @SerializedName("day") val day: Int
)

data class IntervalDto(
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String
)

