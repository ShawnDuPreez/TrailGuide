package com.trailguide.android.services

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.trailguide.android.data.datastore.UserPreferences
import com.trailguide.android.data.dto.FcmTokenRequest
import com.trailguide.android.data.remote.AuthApiService
import com.trailguide.android.data.repository.AuthRepository
import com.trailguide.android.util.NotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var authApiService: AuthApiService
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val prefs by lazy {
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    companion object {
        private const val TAG = "FCMService"
        private const val PREFS_NAME = "trailguide_fcm"
        private const val KEY_FCM_TOKEN = "fcm_token"
        
        // Notification types
        const val TYPE_WEATHER_ALERT = "weather_alert"
        const val TYPE_NEW_TRAIL = "new_trail"
        const val TYPE_FRIEND_REVIEW = "friend_review"
    }
    
    override fun onCreate() {
        super.onCreate()
        // Create notification channels
        NotificationUtil.createNotificationChannels(applicationContext)
        
        // Attempt to upload any cached token
        serviceScope.launch {
            val cachedToken = prefs.getString(KEY_FCM_TOKEN, null)
            if (!cachedToken.isNullOrBlank()) {
                registerTokenWithBackend(cachedToken)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    
    /**
     * Called when a new FCM token is generated.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        storeTokenLocally(token)
        serviceScope.launch {
            registerTokenWithBackend(token)
        }
    }
    
    /**
     * Called when a message is received.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Message received from: ${message.from}")
        
        // Check if notifications are enabled
        serviceScope.launch {
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
    
    private fun storeTokenLocally(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }
    
    private suspend fun registerTokenWithBackend(token: String) {
        val userId = authRepository.currentUser?.id
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "Skipping FCM token upload: user not authenticated")
            return
        }
        
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})"
        try {
            val response = authApiService.updateFcmToken(
                FcmTokenRequest(
                    userId = userId,
                    fcmToken = token,
                    deviceInfo = deviceInfo
                )
            )
            
            if (response.isSuccessful) {
                Log.d(TAG, "FCM token registered with backend")
            } else {
                Log.e(TAG, "Failed to register FCM token: HTTP ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading FCM token: ${e.message}", e)
        }
    }
}

