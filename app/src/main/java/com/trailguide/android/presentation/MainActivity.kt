package com.trailguide.android.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.trailguide.android.data.repository.TrailRepository
import com.trailguide.android.presentation.AuthWrapper
import com.trailguide.android.presentation.theme.TrailGuideTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity for TrailGuide Android app.
 * Entry point for the application using Jetpack Compose.
 * Handles deep links for OAuth callbacks.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var supabaseClient: SupabaseClient
    
    @Inject
    lateinit var trailRepository: TrailRepository
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
}

