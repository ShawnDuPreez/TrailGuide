package com.trailguide.android.data.remote

import com.trailguide.android.data.dto.ForecastResponseDto
import com.trailguide.android.data.dto.WeatherResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for OpenWeather API.
 * Provides current weather and forecast data.
 */
interface WeatherApiService {
    
    /**
     * Get current weather for coordinates.
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Response<WeatherResponseDto>
    
    /**
     * Get 5-day forecast for coordinates.
     */
    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Response<ForecastResponseDto>
}


