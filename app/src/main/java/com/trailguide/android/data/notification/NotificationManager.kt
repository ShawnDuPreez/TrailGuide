package com.trailguide.android.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.trailguide.android.R
import com.trailguide.android.data.model.SafetyRating
import com.trailguide.android.data.model.Weather
import com.trailguide.android.presentation.MainActivity

/**
 * Manages notification channels and displays notifications.
 */
object TrailNotificationManager {
    
    private const val DAILY_WEATHER_CHANNEL_ID = "daily_weather"
    private const val SAFETY_ALERTS_CHANNEL_ID = "safety_alerts"
    
    private const val DAILY_WEATHER_NOTIFICATION_ID = 1001
    private const val SAFETY_ALERT_NOTIFICATION_ID = 1002
    
    /**
     * Create notification channels (required for Android 8.0+).
     * Safe to call multiple times.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            
            // Daily Weather Channel
            val dailyWeatherChannel = NotificationChannel(
                DAILY_WEATHER_CHANNEL_ID,
                context.getString(R.string.daily_weather_channel),
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.daily_weather_channel_description)
                enableVibration(false)
                enableLights(true)
            }
            
            // Safety Alerts Channel
            val safetyAlertsChannel = NotificationChannel(
                SAFETY_ALERTS_CHANNEL_ID,
                context.getString(R.string.safety_alerts_channel),
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.safety_alerts_channel_description)
                enableVibration(true)
                enableLights(true)
            }
            
            notificationManager.createNotificationChannel(dailyWeatherChannel)
            notificationManager.createNotificationChannel(safetyAlertsChannel)
        }
    }
    
    /**
     * Check if app can post notifications (Android 13+).
     */
    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            notificationManager.areNotificationsEnabled()
        } else {
            true // Pre-Android 13, notifications are always allowed
        }
    }
    
    /**
     * Show daily weather notification.
     */
    fun showDailyWeatherNotification(context: Context, weather: Weather, safetyRating: SafetyRating) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
        
        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification content
        val title = context.getString(R.string.good_morning_todays_weather)
        val body = context.getString(
            R.string.weather_notification_body,
            weather.temperature.toInt(),
            weather.description,
            safetyRating.displayName
        )
        
        val notification = NotificationCompat.Builder(context, DAILY_WEATHER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(DAILY_WEATHER_NOTIFICATION_ID, notification)
    }
    
    /**
     * Show safety alert notification for dangerous weather conditions.
     */
    fun showSafetyAlert(context: Context, weather: Weather, @Suppress("UNUSED_PARAMETER") safetyRating: SafetyRating) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
        
        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build alert message based on dangerous conditions
        val alertMessage = buildString {
            when {
                weather.windSpeed > 50 -> append(context.getString(R.string.high_winds_detected, weather.windSpeed.toInt()))
                weather.condition.name == "THUNDERSTORM" -> append(context.getString(R.string.thunderstorm_warning))
                weather.condition.name == "HEAVY_RAIN" -> append(context.getString(R.string.heavy_rain_expected))
                else -> append(context.getString(R.string.dangerous_weather_conditions))
            }
            append(context.getString(R.string.avoid_hiking_trails_today))
            append(context.getString(R.string.current_weather, weather.temperature.toInt(), weather.description))
        }
        
        val notification = NotificationCompat.Builder(context, SAFETY_ALERTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.weather_safety_alert))
            .setContentText(alertMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alertMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()
        
        notificationManager.notify(SAFETY_ALERT_NOTIFICATION_ID, notification)
    }
}
