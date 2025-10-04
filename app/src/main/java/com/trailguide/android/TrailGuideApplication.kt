package com.trailguide.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for TrailGuide Android app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class TrailGuideApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any global application-level components here
    }
}

