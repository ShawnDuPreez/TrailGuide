package com.trailguide.android.data.remote

import com.trailguide.android.data.dto.GoogleForecastResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Google Weather API.
 * Provides current weather and forecast data.
 * Uses the forecast/days:lookup endpoint with proper parameter names.
 */
interface WeatherApiService {
    
    /**
     * Get daily forecast for coordinates.
     * This endpoint returns forecast data including the current day.
     */
    @GET("forecast/days:lookup")
    suspend fun getForecast(
        @Query("key") apiKey: String,
        @Query("location.latitude") latitude: Double,
        @Query("location.longitude") longitude: Double,
        @Query("days") days: Int = 5
    ): Response<GoogleForecastResponseDto>
}


