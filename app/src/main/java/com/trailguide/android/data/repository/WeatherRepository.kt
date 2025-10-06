package com.trailguide.android.data.repository

import com.trailguide.android.BuildConfig
import com.trailguide.android.data.dto.WeatherResponseDto
import com.trailguide.android.data.dto.ForecastResponseDto
import com.trailguide.android.data.model.*
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.remote.WeatherApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for weather data operations.
 * Fetches current weather and forecasts from OpenWeather API.
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApiService: WeatherApiService
) {
    
    companion object {
        private const val API_KEY = BuildConfig.OPENWEATHER_API_KEY
    }
    
    /**
     * Get current weather for a location.
     */
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): Flow<NetworkResult<Weather>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            val response = weatherApiService.getCurrentWeather(
                latitude = latitude,
                longitude = longitude,
                apiKey = API_KEY
            )
            
            if (response.isSuccessful && response.body() != null) {
                val weatherDto = response.body()!!
                val weather = mapWeatherDtoToWeather(weatherDto)
                emit(NetworkResult.Success(weather))
            } else {
                emit(NetworkResult.Error("Failed to fetch weather: ${response.code()}"))
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
            // Fetch current weather
            val currentResponse = weatherApiService.getCurrentWeather(
                latitude = latitude,
                longitude = longitude,
                apiKey = API_KEY
            )
            
            // Fetch forecast
            val forecastResponse = weatherApiService.getForecast(
                latitude = latitude,
                longitude = longitude,
                apiKey = API_KEY
            )
            
            if (currentResponse.isSuccessful && currentResponse.body() != null &&
                forecastResponse.isSuccessful && forecastResponse.body() != null) {
                
                val current = mapWeatherDtoToWeather(currentResponse.body()!!)
                val forecast = mapForecastDtoToForecast(forecastResponse.body()!!)
                val safetyRating = SafetyRating.fromWeather(current)
                
                val weatherForecast = WeatherForecast(
                    current = current,
                    forecast = forecast,
                    trailSafetyRating = safetyRating
                )
                
                emit(NetworkResult.Success(weatherForecast))
            } else {
                emit(NetworkResult.Error("Failed to fetch forecast"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Forecast error: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Map WeatherResponseDto to Weather model.
     */
    private fun mapWeatherDtoToWeather(dto: WeatherResponseDto): Weather {
        val weatherDesc = dto.weather.firstOrNull()?.description ?: "Unknown"
        val weatherIcon = dto.weather.firstOrNull()?.icon ?: "01d"
        
        return Weather(
            temperature = dto.main.temp,
            feelsLike = dto.main.feelsLike,
            humidity = dto.main.humidity,
            windSpeed = dto.wind.speed * 3.6, // Convert m/s to km/h
            description = weatherDesc.replaceFirstChar { it.uppercase() },
            icon = weatherIcon,
            condition = WeatherCondition.fromDescription(weatherDesc)
        )
    }
    
    /**
     * Map ForecastResponseDto to list of DayForecast.
     * Groups by day and takes midday forecast for each day.
     */
    private fun mapForecastDtoToForecast(dto: ForecastResponseDto): List<DayForecast> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        
        // Group forecasts by day
        val dailyForecasts = dto.list
            .groupBy { 
                val date = dateFormat.parse(it.dateTime)
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date ?: Date())
            }
            .entries
            .take(3) // Take only 3 days
            .map { (date, forecasts) ->
                // Get midday forecast (around 12:00)
                val middayForecast = forecasts.minByOrNull { 
                    val time = it.dateTime.substringAfter(" ")
                    kotlin.math.abs(time.substringBefore(":").toInt() - 12)
                } ?: forecasts.first()
                
                val weatherDesc = middayForecast.weather.firstOrNull()?.description ?: "Unknown"
                val icon = middayForecast.weather.firstOrNull()?.icon ?: "01d"
                
                // Calculate min/max from all forecasts for the day
                val temps = forecasts.map { it.main.temp }
                
                DayForecast(
                    date = outputFormat.format(dateFormat.parse(date) ?: Date()),
                    tempMin = temps.minOrNull() ?: middayForecast.main.temp,
                    tempMax = temps.maxOrNull() ?: middayForecast.main.temp,
                    description = weatherDesc.replaceFirstChar { it.uppercase() },
                    icon = icon,
                    precipitationChance = (middayForecast.precipitationProbability * 100).toInt()
                )
            }
        
        return dailyForecasts
    }
}


