package com.trailguide.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.trailguide.android.data.notification.TrailNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for TrailGuide Android app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class TrailGuideApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification channels (required for Android 8.0+)
        // This must be called before any notifications are shown
        TrailNotificationManager.createChannels(this)
        
        // Note: Notification rescheduling is handled by ProfileViewModel when user opens ProfileScreen
        // This ensures proper dependency injection and avoids issues with Application lifecycle
    }
}

