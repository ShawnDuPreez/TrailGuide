package com.trailguide.android.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.trailguide.android.data.datastore.UserPreferences
import com.trailguide.android.util.NotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for handling push notifications.
 */
@AndroidEntryPoint
class TrailGuideMessagingService : FirebaseMessagingService() {
    
    @Inject
    lateinit var userPreferences: UserPreferences
    
    companion object {
        private const val TAG = "FCMService"
        
        // Notification types
        const val TYPE_WEATHER_ALERT = "weather_alert"
        const val TYPE_NEW_TRAIL = "new_trail"
        const val TYPE_FRIEND_REVIEW = "friend_review"
    }
    
    override fun onCreate() {
        super.onCreate()
        // Create notification channels
        NotificationUtil.createNotificationChannels(applicationContext)
    }
    
    /**
     * Called when a new FCM token is generated.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // TODO: Send token to backend
        // This should be done via ProfileRepository or AuthRepository
        // For now, just log it
    }
    
    /**
     * Called when a message is received.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Message received from: ${message.from}")
        
        // Check if notifications are enabled
        CoroutineScope(Dispatchers.IO).launch {
            val notificationsEnabled = userPreferences.notificationsEnabledFlow.first()
            
            if (!notificationsEnabled) {
                Log.d(TAG, "Notifications disabled by user")
                return@launch
            }
            
            // Handle notification data
            message.data.let { data ->
                val type = data["type"] ?: return@launch
                
                when (type) {
                    TYPE_WEATHER_ALERT -> handleWeatherAlert(data)
                    TYPE_NEW_TRAIL -> handleNewTrail(data)
                    TYPE_FRIEND_REVIEW -> handleFriendReview(data)
                    else -> Log.w(TAG, "Unknown notification type: $type")
                }
            }
            
            // Handle notification payload (if sent from Firebase Console)
            message.notification?.let { notification ->
                Log.d(TAG, "Notification title: ${notification.title}")
                Log.d(TAG, "Notification body: ${notification.body}")
            }
        }
    }
    
    /**
     * Handle weather alert notification.
     */
    private suspend fun handleWeatherAlert(data: Map<String, String>) {
        val weatherAlertsEnabled = userPreferences.weatherAlertsFlow.first()
        
        if (!weatherAlertsEnabled) {
            Log.d(TAG, "Weather alerts disabled")
            return
        }
        
        val trailName = data["trail_name"] ?: "Trail"
        val alertMessage = data["message"] ?: "Weather alert"
        
        NotificationUtil.showWeatherAlert(
            applicationContext,
            trailName,
            alertMessage
        )
    }
    
    /**
     * Handle new trail notification.
     */
    private suspend fun handleNewTrail(data: Map<String, String>) {
        val newTrailsEnabled = userPreferences.newTrailsFlow.first()
        
        if (!newTrailsEnabled) {
            Log.d(TAG, "New trail notifications disabled")
            return
        }
        
        val trailName = data["trail_name"] ?: "Unknown Trail"
        val location = data["location"] ?: "Unknown Location"
        
        NotificationUtil.showNewTrailNotification(
            applicationContext,
            trailName,
            location
        )
    }
    
    /**
     * Handle friend review notification.
     */
    private suspend fun handleFriendReview(data: Map<String, String>) {
        val friendActivityEnabled = userPreferences.friendActivityFlow.first()
        
        if (!friendActivityEnabled) {
            Log.d(TAG, "Friend activity notifications disabled")
            return
        }
        
        val friendName = data["friend_name"] ?: "A friend"
        val trailName = data["trail_name"] ?: "a trail"
        
        NotificationUtil.showFriendReviewNotification(
            applicationContext,
            friendName,
            trailName
        )
    }
}

