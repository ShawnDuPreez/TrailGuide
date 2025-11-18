package com.trailguide.android.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.trailguide.android.data.security.BiometricAuthenticationManager
import com.trailguide.android.presentation.navigation.TrailGuideApp
import com.trailguide.android.presentation.screens.LoginScreen
import com.trailguide.android.presentation.screens.RegisterScreen
import com.trailguide.android.presentation.viewmodel.AuthStateViewModel
import com.trailguide.android.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val isAuthenticated by authStateViewModel.isAuthenticated.collectAsState()
    val isLoading by authStateViewModel.isLoading.collectAsState()
    val currentUser by authStateViewModel.currentUser.collectAsState()
    val authError by authStateViewModel.authError.collectAsState()
    val biometricGatePassed by authStateViewModel.biometricGatePassed.collectAsState()
    val userPreferences by authStateViewModel.userPreferences.collectAsState()
    val isBiometricAvailable = remember { authStateViewModel.isBiometricAvailable() }
    
    // Refresh auth state when the app becomes active (useful for OAuth callbacks)
    LaunchedEffect(Unit) {
        // Small delay to allow OAuth callbacks to be processed
        kotlinx.coroutines.delay(1000)
        authStateViewModel.refreshAuthState()
    }
    
    // Watch for authentication state changes to immediately update UI
    // This ensures instant redirect when sign out happens
    // No delay needed - state is already cleared synchronously in signOut()
    // The when statement below will react immediately to the state change
    
    // Check if biometric gate is required
    val requiresBiometricGate = isAuthenticated && 
                                currentUser != null && 
                                isBiometricAvailable && 
                                userPreferences.biometricsEnabled && 
                                !biometricGatePassed
    
    // Track if we've already attempted biometric auth to avoid multiple prompts
    var biometricAttempted by remember { mutableStateOf(false) }
    
    // Handle biometric authentication when required
    // Only trigger after loading is complete and we have all required state
    LaunchedEffect(requiresBiometricGate, biometricGatePassed, isLoading) {
        // Wait for loading to complete before showing biometric prompt
        if (!isLoading && requiresBiometricGate && !biometricAttempted) {
            val activity = context as? FragmentActivity
            if (activity == null) {
                // Don't sign out on context error - just skip biometric
                return@LaunchedEffect
            }
            
            // Small delay to ensure UI is ready
            kotlinx.coroutines.delay(500)
            biometricAttempted = true
            
            val biometricManager = BiometricAuthenticationManager(context)
            
            try {
                // Check if biometric is available first
                if (!biometricManager.canUseBiometric()) {
                    // If biometric becomes unavailable, just skip the gate
                    authStateViewModel.passBiometricGate()
                    return@LaunchedEffect
                }
                
                val authResult = biometricManager.authenticateWithBiometric(
                    activity = activity,
                    title = "Unlock TrailGuide",
                    subtitle = "Use your fingerprint or face to access the app"
                )
                
                if (authResult) {
                    authStateViewModel.passBiometricGate()
                } else {
                    // If biometric fails or is cancelled, sign out the user for security
                    authStateViewModel.signOut()
                }
            } catch (e: Exception) {
                // If there's an error, sign out for security
                authStateViewModel.signOut()
            }
        } else if (!requiresBiometricGate) {
            // Reset the flag when biometric is no longer required
            biometricAttempted = false
        }
    }
    
    // Determine what to show based on authentication state
    // Priority: Loading > Not Authenticated > Biometric Gate > Authenticated
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
        
        !isAuthenticated || currentUser == null -> {
            // User is not authenticated - show authentication screens immediately
            // This ensures instant redirect when sign out happens
            AuthenticationFlow(
                onAuthSuccess = { user ->
                    authStateViewModel.signIn(user)
                }
            )
        }
        
        requiresBiometricGate -> {
            // Show biometric prompt screen with manual trigger button
            val coroutineScope = rememberCoroutineScope()
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Biometric Authentication Required",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please use your fingerprint or face to unlock the app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    // Manual trigger button as fallback
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val activity = context as? FragmentActivity
                                    if (activity == null) {
                                        return@launch
                                    }
                                    
                                    val biometricManager = BiometricAuthenticationManager(context)
                                    
                                    // Check if biometric is available first
                                    if (!biometricManager.canUseBiometric()) {
                                        // If biometric becomes unavailable, skip the gate
                                        authStateViewModel.passBiometricGate()
                                        return@launch
                                    }
                                    
                                    val authResult = biometricManager.authenticateWithBiometric(
                                        activity = activity,
                                        title = "Unlock TrailGuide",
                                        subtitle = "Use your fingerprint or face to access the app"
                                    )
                                    
                                    if (authResult) {
                                        authStateViewModel.passBiometricGate()
                                    } else {
                                        // If biometric fails or is cancelled, sign out for security
                                        authStateViewModel.signOut()
                                    }
                                } catch (e: Exception) {
                                    // If there's an error, sign out for security
                                    authStateViewModel.signOut()
                                }
                            }
                        }
                    ) {
                        Text("Authenticate")
                    }
                }
            }
        }
        
        else -> {
            // User is authenticated and biometric gate passed (if required), show main app
            TrailGuideApp()
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
