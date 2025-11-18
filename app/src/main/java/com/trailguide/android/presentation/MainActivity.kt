package com.trailguide.android.presentation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.trailguide.android.data.model.Language
import com.trailguide.android.data.repository.PreferencesRepository
import com.trailguide.android.data.repository.TrailRepository
import com.trailguide.android.presentation.AuthWrapper
import com.trailguide.android.presentation.theme.TrailGuideTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Main Activity for TrailGuide Android app.
 * Entry point for the application using Jetpack Compose.
 * Handles deep links for OAuth callbacks.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var supabaseClient: SupabaseClient
    
    @Inject
    lateinit var trailRepository: TrailRepository
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Read language preference synchronously from SharedPreferences cache
        val sharedPrefs = newBase.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val languageCode = sharedPrefs.getString("language", "en") ?: "en"
        
        val locale = when (languageCode) {
            "af" -> Locale("af")
            "zu" -> Locale("zu")
            else -> Locale("en")
        }
        
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Update locale on startup
        updateLocale()
        
        // Wake up the Render server in the background (for free tier cold starts)
        lifecycleScope.launch {
            trailRepository.wakeUpServer()
        }
        
        // Handle deep link if activity was started with one
        handleDeepLink(intent)
        
        setContent {
            TrailGuideTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthWrapper()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle deep link when activity receives a new intent (e.g., OAuth callback)
        handleDeepLink(intent)
    }
    
    /**
     * Handle deep link intents for OAuth callbacks.
     */
    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        
        if (data != null) {
            Log.d(TAG, "Deep link received: $data")
            
            // Check if this is an auth callback
            if (data.scheme == "trailguide" && data.host == "auth-callback") {
                Log.d(TAG, "Auth callback detected - processing with Supabase")
                
                // Handle the OAuth callback with Supabase
                lifecycleScope.launch {
                    try {
                        supabaseClient.handleDeeplinks(intent) { userSession ->
                            Log.d(TAG, "OAuth callback processed successfully: ${userSession.user?.email}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling OAuth callback", e)
                    }
                }
            }
        }
    }
    
    /**
     * Update app locale based on user preference.
     */
    private fun updateLocale() {
        lifecycleScope.launch {
            try {
                val preferences = preferencesRepository.userPreferencesFlow.first()
                val locale = when (preferences.language) {
                    Language.ENGLISH -> Locale("en")
                    Language.AFRIKAANS -> Locale("af")
                    Language.ZULU -> Locale("zu")
                }
                
                Locale.setDefault(locale)
                val config = Configuration(resources.configuration)
                config.setLocale(locale)
                resources.updateConfiguration(config, resources.displayMetrics)
            } catch (e: Exception) {
                // Default to English if error
                Locale.setDefault(Locale("en"))
            }
        }
    }
}

