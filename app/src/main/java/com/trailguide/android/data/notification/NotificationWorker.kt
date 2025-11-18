package com.trailguide.android.data.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trailguide.android.data.model.SafetyRating
import com.trailguide.android.data.notification.TrailNotificationManager
import com.trailguide.android.data.repository.LocationRepository
import com.trailguide.android.data.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that runs daily at specified time to fetch weather and show notifications.
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "NotificationWorker"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "NotificationWorker started")
        
        return try {
            // Get user's location (or default)
            val (latitude, longitude) = locationRepository.getLocation()
            Log.d(TAG, "Using location: lat=$latitude, lon=$longitude")
            
            // Fetch current weather
            Log.d(TAG, "Fetching weather data...")
            // Wait for the first non-Loading result
            val weatherResult = weatherRepository.getCurrentWeather(latitude, longitude)
                .first { it !is com.trailguide.android.data.remote.NetworkResult.Loading }
            
            // Process the result (Loading is filtered out, so we only get Success or Error)
            when (weatherResult) {
                is com.trailguide.android.data.remote.NetworkResult.Success -> {
                    val weather = weatherResult.data
                    val safetyRating = SafetyRating.fromWeather(weather)
                    
                    Log.d(TAG, "Weather fetched successfully:")
                    Log.d(TAG, "  Temperature: ${weather.temperature}°C")
                    Log.d(TAG, "  Description: ${weather.description}")
                    Log.d(TAG, "  Wind Speed: ${weather.windSpeed} km/h")
                    Log.d(TAG, "  Safety Rating: ${safetyRating.displayName}")
                    
                    // Always show daily weather notification
                    Log.d(TAG, "Showing daily weather notification...")
                    TrailNotificationManager.showDailyWeatherNotification(
                        context,
                        weather,
                        safetyRating
                    )
                    Log.d(TAG, "Daily weather notification shown successfully")
                    
                    // Show safety alert if conditions are dangerous
                    if (safetyRating == SafetyRating.DANGEROUS || safetyRating == SafetyRating.POOR) {
                        Log.d(TAG, "Dangerous conditions detected, showing safety alert...")
                        TrailNotificationManager.showSafetyAlert(
                            context,
                            weather,
                            safetyRating
                        )
                        Log.d(TAG, "Safety alert shown successfully")
                    }
                    
                    Log.d(TAG, "NotificationWorker completed successfully")
                    Result.success()
                }
                is com.trailguide.android.data.remote.NetworkResult.Error -> {
                    Log.e(TAG, "Failed to fetch weather: ${weatherResult.message}")
                    weatherResult.exception?.let { 
                        Log.e(TAG, "Exception details:", it)
                    }
                    // Log error but don't fail - will retry on next scheduled run
                    Result.retry()
                }
                is com.trailguide.android.data.remote.NetworkResult.Loading -> {
                    // This shouldn't happen since we filtered it out, but handle it just in case
                    Log.w(TAG, "Unexpected Loading state received, retrying...")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in NotificationWorker:", e)
            // Retry on exception
            Result.retry()
        }
    }
}

