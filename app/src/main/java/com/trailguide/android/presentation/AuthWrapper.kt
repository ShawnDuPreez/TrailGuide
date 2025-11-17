package com.trailguide.android.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.trailguide.android.presentation.navigation.TrailGuideApp
import com.trailguide.android.presentation.screens.LoginScreen
import com.trailguide.android.presentation.screens.RegisterScreen
import com.trailguide.android.presentation.viewmodel.AuthStateViewModel
import com.trailguide.android.presentation.viewmodel.AuthViewModel

/**
 * Authentication wrapper that handles the authentication flow.
 * Shows login/register screens when user is not authenticated,
 * and the main app when authenticated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthWrapper(
    navController: NavHostController = rememberNavController(),
    authStateViewModel: AuthStateViewModel = hiltViewModel()
) {
    val isAuthenticated by authStateViewModel.isAuthenticated.collectAsState()
    val isLoading by authStateViewModel.isLoading.collectAsState()
    val currentUser by authStateViewModel.currentUser.collectAsState()
    val authError by authStateViewModel.authError.collectAsState()
    
    // Check auth state when authenticated state changes to detect sign out
    // This will trigger when ProfileViewModel signs out
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            // User is authenticated - check periodically if they signed out
            // Use a single delayed check instead of a loop to avoid flashing
            kotlinx.coroutines.delay(2000)
            authStateViewModel.refreshAuthState()
        }
    }
    
    // Initial check on app start (for OAuth callbacks)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        authStateViewModel.refreshAuthState()
    }
    
    when {
        isLoading -> {
            // Show loading screen while checking authentication
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading TrailGuide...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        isAuthenticated && currentUser != null -> {
            // User is authenticated, show main app
            TrailGuideApp()
        }
        
        else -> {
            // User is not authenticated, show authentication screens
            AuthenticationFlow(
                onAuthSuccess = { user ->
                    authStateViewModel.signIn(user)
                }
            )
        }
    }
    
    // Show error dialog if there's an authentication error
    authError?.let { error ->
        AlertDialog(
            onDismissRequest = { authStateViewModel.clearError() },
            title = { Text("Authentication Error") },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = { authStateViewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Authentication flow composable that handles login/register screens.
 */
@Composable
private fun AuthenticationFlow(
    onAuthSuccess: (com.trailguide.android.data.model.User) -> Unit
) {
    var isLoginScreen by remember { mutableStateOf(true) }
    
    if (isLoginScreen) {
        LoginScreen(
            onNavigateToRegister = { isLoginScreen = false },
            onLoginSuccess = { user ->
                onAuthSuccess(user)
            }
        )
    } else {
        RegisterScreen(
            onNavigateToLogin = { isLoginScreen = true },
            onRegisterSuccess = { user ->
                onAuthSuccess(user)
            }
        )
    }
}
