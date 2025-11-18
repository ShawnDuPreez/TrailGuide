package com.trailguide.android.data.repository

import com.trailguide.android.BuildConfig
import com.trailguide.android.data.dto.GoogleForecastResponseDto
import com.trailguide.android.data.model.*
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.remote.WeatherApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for weather data operations.
 * Fetches current weather and forecasts from Google Weather API.
 * Note: Google Weather API uses forecast endpoint for both current and forecast data.
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApiService: WeatherApiService
) {
    
    companion object {
        private const val API_KEY = BuildConfig.GOOGLE_WEATHER_API_KEY
    }
    
    /**
     * Get current weather for a location.
     * Uses forecast endpoint and extracts current conditions from first day.
     */
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): Flow<NetworkResult<Weather>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            // Fetch forecast (first day contains current conditions)
            val forecastResponse = weatherApiService.getForecast(
                apiKey = API_KEY,
                latitude = latitude,
                longitude = longitude,
                days = 1
            )
            
            if (forecastResponse.isSuccessful && forecastResponse.body() != null) {
                val forecastDto = forecastResponse.body()!!
                val firstDay = forecastDto.forecastDays.firstOrNull()
                
                if (firstDay != null) {
                    val daytimeForecast = firstDay.daytimeForecast
                    val windSpeed = daytimeForecast?.wind?.speed?.value ?: 0.0
                    
                    val weather = mapForecastDayToWeather(firstDay, windSpeed)
                    emit(NetworkResult.Success(weather))
                } else {
                    emit(NetworkResult.Error("No forecast data available"))
                }
            } else {
                emit(NetworkResult.Error("Failed to fetch weather: ${forecastResponse.code()}"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Weather error: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get weather forecast with safety rating.
     */
    suspend fun getWeatherForecast(latitude: Double, longitude: Double): Flow<NetworkResult<WeatherForecast>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            // Fetch forecast (includes current day)
            val forecastResponse = weatherApiService.getForecast(
                apiKey = API_KEY,
                latitude = latitude,
                longitude = longitude,
                days = 5
            )
            
            if (forecastResponse.isSuccessful && forecastResponse.body() != null) {
                val forecastDto = forecastResponse.body()!!
                val firstDay = forecastDto.forecastDays.firstOrNull()
                
                if (firstDay != null) {
                    // Get wind speed from first day
                    val windSpeed = firstDay.daytimeForecast?.wind?.speed?.value ?: 0.0
                    
                    // Map first day as current weather
                    val current = mapForecastDayToWeather(firstDay, windSpeed)
                    
                    // Map remaining days as forecast (skip first day)
                    val forecast = mapGoogleForecastToForecast(forecastDto)
                    val safetyRating = SafetyRating.fromWeather(current)
                    
                    val weatherForecast = WeatherForecast(
                        current = current,
                        forecast = forecast,
                        trailSafetyRating = safetyRating
                    )
                    
                    emit(NetworkResult.Success(weatherForecast))
                } else {
                    emit(NetworkResult.Error("No forecast data available"))
                }
            } else {
                val errorBody = try {
                    forecastResponse.errorBody()?.string() ?: "Unknown error"
                } catch (e: Exception) {
                    "Could not read error body"
                }
                emit(NetworkResult.Error("Failed to fetch forecast: HTTP ${forecastResponse.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Forecast error: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Map ForecastDayDto to Weather model (for current conditions).
     */
    private fun mapForecastDayToWeather(
        day: com.trailguide.android.data.dto.ForecastDayDto,
        windSpeed: Double
    ): Weather {
        val daytimeForecast = day.daytimeForecast
        val description = daytimeForecast?.weatherCondition?.description?.text ?: "Unknown"
        val conditionType = daytimeForecast?.weatherCondition?.type ?: "CLEAR"
        val icon = daytimeForecast?.weatherCondition?.iconBaseUri ?: ""
        
        // Use max temperature as current temperature (or average of min/max)
        val currentTemp = (day.maxTemperature.degrees + day.minTemperature.degrees) / 2.0
        val feelsLike = day.feelsLikeMaxTemperature?.degrees ?: currentTemp
        
        return Weather(
            temperature = currentTemp,
            feelsLike = feelsLike,
            humidity = daytimeForecast?.relativeHumidity ?: 0,
            windSpeed = windSpeed, // Already in km/h from Google API
            description = description.replaceFirstChar { it.uppercase() },
            icon = icon,
            condition = WeatherCondition.fromGoogleType(conditionType)
        )
    }
    
    /**
     * Map GoogleForecastResponseDto to list of DayForecast.
     * Skips the first day (current day) and returns next 3 days.
     */
    private fun mapGoogleForecastToForecast(dto: GoogleForecastResponseDto): List<DayForecast> {
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
        
        // Skip first day (current) and take next 3 days
        return dto.forecastDays.drop(1).take(3).map { day ->
            val daytimeForecast = day.daytimeForecast
            val description = daytimeForecast?.weatherCondition?.description?.text ?: "Unknown"
            val conditionType = daytimeForecast?.weatherCondition?.type ?: "CLEAR"
            val icon = daytimeForecast?.weatherCondition?.iconBaseUri ?: ""
            
            // Format date from displayDate
            val date = try {
                LocalDate.of(day.displayDate.year, day.displayDate.month, day.displayDate.day)
                    .format(dateFormatter)
            } catch (e: Exception) {
                "${day.displayDate.month}/${day.displayDate.day}"
            }
            
            DayForecast(
                date = date,
                tempMin = day.minTemperature.degrees,
                tempMax = day.maxTemperature.degrees,
                description = description.replaceFirstChar { it.uppercase() },
                icon = icon,
                precipitationChance = daytimeForecast?.precipitation?.probability?.percent ?: 0
            )
        }
    }
}


