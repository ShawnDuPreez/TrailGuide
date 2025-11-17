package com.trailguide.android.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.trailguide.android.R
import com.trailguide.android.presentation.MainActivity

/**
 * Utility class for creating and managing notifications.
 */
object NotificationUtil {
    
    // Notification channels
    const val CHANNEL_WEATHER = "weather_alerts"
    const val CHANNEL_NEW_TRAILS = "new_trails"
    const val CHANNEL_FRIEND_ACTIVITY = "friend_activity"
    const val CHANNEL_SYNC = "sync_status"
    
    // Notification IDs
    const val NOTIFICATION_ID_WEATHER = 1001
    const val NOTIFICATION_ID_NEW_TRAIL = 1002
    const val NOTIFICATION_ID_FRIEND_REVIEW = 1003
    const val NOTIFICATION_ID_SYNC = 1004
    
    /**
     * Create notification channels (required for Android O+).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Weather alerts channel
            val weatherChannel = NotificationChannel(
                CHANNEL_WEATHER,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for weather alerts on trails"
                enableVibration(true)
            }
            
            // New trails channel
            val newTrailsChannel = NotificationChannel(
                CHANNEL_NEW_TRAILS,
                "New Trails",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new trails in your area"
            }
            
            // Friend activity channel
            val friendActivityChannel = NotificationChannel(
                CHANNEL_FRIEND_ACTIVITY,
                "Friend Activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for friend reviews and activity"
            }
            
            // Sync status channel
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Sync Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sync status updates"
            }
            
            notificationManager.createNotificationChannels(
                listOf(weatherChannel, newTrailsChannel, friendActivityChannel, syncChannel)
            )
        }
    }
    
    /**
     * Show weather alert notification.
     */
    fun showWeatherAlert(
        context: Context,
        trailName: String,
        alertMessage: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_WEATHER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Weather Alert: $trailName")
            .setContentText(alertMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alertMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_WEATHER, notification)
    }
    
    /**
     * Show new trail notification.
     */
    fun showNewTrailNotification(
        context: Context,
        trailName: String,
        location: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_TRAILS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New Trail Available")
            .setContentText("$trailName in $location")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Discover $trailName in $location"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_NEW_TRAIL, notification)
    }
    
    /**
     * Show friend review notification.
     */
    fun showFriendReviewNotification(
        context: Context,
        friendName: String,
        trailName: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_FRIEND_ACTIVITY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New Review from $friendName")
            .setContentText("$friendName reviewed $trailName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_FRIEND_REVIEW, notification)
    }
    
    /**
     * Show sync status notification.
     */
    fun showSyncNotification(
        context: Context,
        isSyncing: Boolean,
        success: Boolean = true
    ) {
        val title = if (isSyncing) {
            "Syncing..."
        } else if (success) {
            "Sync Complete"
        } else {
            "Sync Failed"
        }
        
        val message = if (isSyncing) {
            "Syncing your data"
        } else if (success) {
            "All data synced successfully"
        } else {
            "Failed to sync. Will retry later."
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isSyncing)
            .setAutoCancel(!isSyncing)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_SYNC, notification)
    }
    
    /**
     * Cancel sync notification.
     */
    fun cancelSyncNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_SYNC)
    }
}

