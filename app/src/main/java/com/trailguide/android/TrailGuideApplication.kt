package com.trailguide.android

import android.app.Application
import android.content.Context
import android.content.res.Configuration as AndroidConfiguration
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.trailguide.android.data.model.Language
import com.trailguide.android.data.notification.TrailNotificationManager
import com.trailguide.android.data.repository.PreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Application class for TrailGuide Android app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class TrailGuideApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification channels (required for Android 8.0+)
        // This must be called before any notifications are shown
        TrailNotificationManager.createChannels(this)
        
        // Set locale based on user preference
        applicationScope.launch {
            try {
                val preferences = preferencesRepository.userPreferencesFlow.first()
                updateLocale(preferences.language)
                // Initialize SharedPreferences cache for synchronous access
                val sharedPrefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
                if (!sharedPrefs.contains("language")) {
                    sharedPrefs.edit().putString("language", preferences.language.code).apply()
                }
            } catch (e: Exception) {
                // Default to English if error
                updateLocale(Language.ENGLISH)
                val sharedPrefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
                sharedPrefs.edit().putString("language", "en").apply()
            }
        }
        
        // Note: Notification rescheduling is handled by ProfileViewModel when user opens ProfileScreen
        // This ensures proper dependency injection and avoids issues with Application lifecycle
    }
    
    /**
     * Update app locale based on language preference.
     */
    private fun updateLocale(language: Language) {
        val locale = when (language) {
            Language.ENGLISH -> Locale("en")
            Language.AFRIKAANS -> Locale("af")
            Language.ZULU -> Locale("zu")
        }
        
        Locale.setDefault(locale)
        val config = AndroidConfiguration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    
    /**
     * Update locale when language changes (called from ProfileViewModel).
     */
    fun updateAppLocale(language: Language) {
        updateLocale(language)
    }
    
    override fun attachBaseContext(base: Context) {
        // This is called before onCreate, so we can't use Hilt injection here
        // We'll set locale in onCreate instead
        super.attachBaseContext(base)
    }
}

