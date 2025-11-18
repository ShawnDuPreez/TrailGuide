package com.trailguide.android.data.repository

import android.util.Log
import com.trailguide.android.data.model.AuthProvider
import com.trailguide.android.data.model.User
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.security.BiometricAuthenticationManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.ktor.util.reflect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations using Supabase Auth.
 * Handles user sign-in, sign-up, and session management.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val biometricAuthManager: BiometricAuthenticationManager
) {
    
    companion object {
        private const val TAG = "AuthRepository"
    }
    
    /**
     * Get the currently signed-in user from Supabase session.
     */
    val currentUser: io.github.jan.supabase.gotrue.user.UserInfo?
        get() = supabaseClient.auth.currentUserOrNull()
    
    /**
     * Sign in with Google using Supabase Auth.
     * This initiates the OAuth flow - the actual authentication happens via deep link callback.
     */
    suspend fun signInWithGoogle(): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Initiating Google OAuth flow")
            
            // Initiate Google OAuth flow - this will open browser
            supabaseClient.auth.signInWith(Google)
            
            // Don't check for user immediately - OAuth happens asynchronously via browser
            // The MainActivity will handle the callback and establish the session
            emit(NetworkResult.Success(User(
                id = "",
                email = "Redirecting...",
                displayName = "Opening browser...",
                photoUrl = null,
                provider = AuthProvider.GOOGLE
            )))
            Log.d(TAG, "OAuth flow initiated - waiting for callback")
            
        } catch (e: Exception) {
            emit(NetworkResult.Error("Google sign-in failed: ${e.message}", e))
            Log.e(TAG, "Google sign-in error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Sign in with email and password using Supabase Auth.
     */
    suspend fun signInWithEmail(email: String, password: String): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            // Wait a moment for session to be established
            kotlinx.coroutines.delay(300)
            
            val supabaseUser = supabaseClient.auth.currentUserOrNull()
            
            if (supabaseUser != null) {
                val user = User(
                    id = supabaseUser.id,
                    email = supabaseUser.email ?: email,
                    displayName = supabaseUser.userMetadata?.get("full_name")?.jsonPrimitive?.content
                        ?: supabaseUser.userMetadata?.get("display_name")?.jsonPrimitive?.content
                        ?: email.substringBefore("@"),
                    photoUrl = supabaseUser.userMetadata?.get("avatar_url")?.jsonPrimitive?.content,
                    provider = AuthProvider.EMAIL
                )
                
                emit(NetworkResult.Success(user))
                Log.d(TAG, "Successfully signed in with email: ${user.email}")
            } else {
                emit(NetworkResult.Error("Sign-in succeeded but session not available. Try again."))
                Log.e(TAG, "Supabase user is null after sign-in")
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Invalid login credentials") == true -> 
                    "Invalid email or password. If you signed up with Google, use Google sign-in instead."
                e.message?.contains("Email not confirmed") == true ->
                    "Please check your email and confirm your account first."
                else -> "Sign-in failed: ${e.message}"
            }
            emit(NetworkResult.Error(errorMessage, e))
            Log.e(TAG, "Email sign-in error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Register new user with email and password using Supabase Auth.
     */
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("full_name", displayName)
                    put("display_name", displayName)
                }
            }
            
            // Wait for registration to complete
            kotlinx.coroutines.delay(500)
            
            val supabaseUser = supabaseClient.auth.currentUserOrNull()
            
            if (supabaseUser != null) {
                val user = User(
                    id = supabaseUser.id,
                    email = email,
                    displayName = displayName,
                    photoUrl = null,
                    provider = AuthProvider.EMAIL
                )
                
                emit(NetworkResult.Success(user))
                Log.d(TAG, "Successfully registered user: $email")
            } else {
                // Registration succeeded but auto-login failed (might need email confirmation)
                emit(NetworkResult.Success(User(
                    id = "",
                    email = email,
                    displayName = displayName,
                    photoUrl = null,
                    provider = AuthProvider.EMAIL
                )))
                Log.d(TAG, "Registration completed, check email for confirmation: $email")
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("after 59 seconds") == true || 
                e.message?.contains("60 seconds") == true ->
                    "Too many attempts. Please wait 1 minute and try again."
                e.message?.contains("already registered") == true ||
                e.message?.contains("already exists") == true ->
                    "This email is already registered. Try signing in instead."
                e.message?.contains("Password") == true ->
                    "Password must be at least 6 characters long."
                else -> "Registration failed: ${e.message}"
            }
            emit(NetworkResult.Error(errorMessage, e))
            Log.e(TAG, "Registration error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Sign out the current user from Supabase.
     * Handles invalid/expired sessions gracefully by clearing local state.
     */
    suspend fun signOut(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            // Try to sign out via Supabase API
            val currentUser = supabaseClient.auth.currentUserOrNull()
            
            if (currentUser != null) {
                Log.d(TAG, "Signing out user: ${currentUser.email}")
                try {
                    supabaseClient.auth.signOut()
                    Log.d(TAG, "User signed out successfully via API")
                } catch (signOutError: Exception) {
                    // If signOut() fails, the session might already be invalid
                    // We'll still clear local state below
                    Log.w(TAG, "Supabase signOut() failed, but continuing with local cleanup: ${signOutError.message}")
                }
            } else {
                Log.w(TAG, "No user session found, clearing local state")
            }
            
            // Always clear biometric credentials and local state, regardless of API call success
            clearBiometricCredentials()
            emit(NetworkResult.Success(Unit))
            
        } catch (e: Exception) {
            // Handle various session/JWT errors gracefully
            // These errors indicate the session is already invalid, so we just need to clear local state
            val errorMessage = e.message?.lowercase() ?: ""
            if (errorMessage.contains("sub claim in jwt does not exist") ||
                errorMessage.contains("invalid refresh token") ||
                errorMessage.contains("session from sessionid claim in jwt does not exist") ||
                errorMessage.contains("sessionid") ||
                (errorMessage.contains("jwt") && errorMessage.contains("does not exist"))) {
                Log.w(TAG, "Session already invalid, clearing local state: ${e.message}")
            } else {
                Log.w(TAG, "Sign-out error, but clearing local state anyway: ${e.message}")
            }
            
            // Always clear biometric credentials and local state, even on error
            clearBiometricCredentials()
            emit(NetworkResult.Success(Unit))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Check if user is signed in.
     */
    fun isSignedIn(): Boolean {
        return supabaseClient.auth.currentUserOrNull() != null
    }
    
    /**
     * Get current user as domain model.
     */
    fun getCurrentUserModel(): User? {
        val supabaseUser = supabaseClient.auth.currentUserOrNull() ?: return null
        
        return User(
            id = supabaseUser.id,
            email = supabaseUser.email ?: "",
            displayName = supabaseUser.userMetadata?.get("full_name")?.jsonPrimitive?.content,
            photoUrl = supabaseUser.userMetadata?.get("avatar_url")?.jsonPrimitive?.content,
            provider = when {
                supabaseUser.appMetadata?.get("provider")?.jsonPrimitive?.content == "google" -> AuthProvider.GOOGLE
                else -> AuthProvider.EMAIL
            }
        )
    }
    
    /**
     * Refresh the current session.
     */
    suspend fun refreshSession(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            supabaseClient.auth.refreshCurrentSession()
            emit(NetworkResult.Success(Unit))
            Log.d(TAG, "Session refreshed successfully")
        } catch (e: Exception) {
            emit(NetworkResult.Error("Session refresh failed: ${e.message}", e))
            Log.e(TAG, "Session refresh error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Check if biometric authentication is available on the device.
     */
    fun isBiometricAvailable(): Boolean {
        return biometricAuthManager.canUseBiometric()
    }
    
    /**
     * Check if biometric credentials are stored for the current user.
     */
    fun hasBiometricCredentials(): Boolean {
        return biometricAuthManager.hasStoredCredentials()
    }
    
    /**
     * Store user credentials securely using biometric authentication.
     * For email/password users, stores email and password.
     * For SSO users, stores the refresh token.
     * @param activity FragmentActivity needed for biometric prompt
     * @param email User's email (for email/password users)
     * @param password User's password (for email/password users, null for SSO)
     * @return Flow with success/error result
     */
    suspend fun storeBiometricCredentials(activity: androidx.fragment.app.FragmentActivity, email: String, password: String?): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            if (!biometricAuthManager.canUseBiometric()) {
                emit(NetworkResult.Error("Biometric authentication is not available on this device"))
                return@flow
            }
            
            val success = if (password != null) {
                // Email/password user - store credentials
                biometricAuthManager.storeCredentialsWithBiometric(activity, email, password)
            } else {
                // SSO user - store refresh token
                val currentSession = supabaseClient.auth.currentSessionOrNull()
                if (currentSession != null) {
                    val refreshToken = currentSession.refreshToken
                    biometricAuthManager.storeSessionTokenWithBiometric(activity, refreshToken)
                } else {
                    emit(NetworkResult.Error("No active session found. Please sign in first."))
                    return@flow
                }
            }
            
            if (success) {
                emit(NetworkResult.Success(Unit))
                Log.d(TAG, "Biometric credentials stored successfully")
            } else {
                emit(NetworkResult.Error("Failed to store biometric credentials"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Failed to store biometric credentials: ${e.message}", e))
            Log.e(TAG, "Error storing biometric credentials", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Sign in using biometric authentication.
     * This retrieves stored credentials (email/password or SSO refresh token) and signs in with Supabase.
     * @param activity FragmentActivity needed for biometric prompt
     * @return Flow with User result
     */
    suspend fun signInWithBiometric(activity: androidx.fragment.app.FragmentActivity): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            if (!biometricAuthManager.canUseBiometric()) {
                emit(NetworkResult.Error("Biometric authentication is not available"))
                return@flow
            }
            
            // Authenticate with biometric
            val authResult = biometricAuthManager.authenticateWithBiometric(
                activity = activity,
                title = "Sign in to TrailGuide",
                subtitle = "Use your biometric to sign in"
            )
            
            if (!authResult) {
                emit(NetworkResult.Error("Biometric authentication failed"))
                return@flow
            }
            
            // Retrieve stored credentials
            val credentials = biometricAuthManager.retrieveCredentialsWithBiometric(activity)
            
            if (credentials == null) {
                emit(NetworkResult.Error("No stored credentials found"))
                return@flow
            }
            
            when (credentials) {
                is com.trailguide.android.data.security.BiometricCredentials.EmailPassword -> {
                    // Sign in with email/password
                    supabaseClient.auth.signInWith(Email) {
                        this.email = credentials.email
                        this.password = credentials.password
                    }
                }
                is com.trailguide.android.data.security.BiometricCredentials.SessionToken -> {
                    // For SSO users, Supabase SDK should automatically persist and restore sessions
                    // We just need to check if a valid session exists and refresh it if needed
                    try {
                        val currentSession = supabaseClient.auth.currentSessionOrNull()
                        if (currentSession == null) {
                            // No active session - Supabase SDK should have restored it if it was valid
                            // If not restored, the session has expired and user needs to sign in again
                            emit(NetworkResult.Error("Session expired. Please sign in again."))
                            return@flow
                        } else {
                            // Session exists, refresh it to ensure it's valid
                            try {
                                supabaseClient.auth.refreshCurrentSession()
                            } catch (e: Exception) {
                                // Refresh failed - session might be expired
                                // Try to continue anyway as the session might still be valid
                                Log.w(TAG, "Session refresh failed, but continuing: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        emit(NetworkResult.Error("Biometric authentication failed: ${e.message}"))
                        return@flow
                    }
                }
            }
            
            // Wait for session to be established
            kotlinx.coroutines.delay(300)
            
            val supabaseUser = supabaseClient.auth.currentUserOrNull()
            
            if (supabaseUser != null) {
                val user = User(
                    id = supabaseUser.id,
                    email = supabaseUser.email ?: "",
                    displayName = supabaseUser.userMetadata?.get("full_name")?.jsonPrimitive?.content
                        ?: supabaseUser.userMetadata?.get("display_name")?.jsonPrimitive?.content
                        ?: supabaseUser.email?.substringBefore("@"),
                    photoUrl = supabaseUser.userMetadata?.get("avatar_url")?.jsonPrimitive?.content,
                    provider = when (credentials) {
                        is com.trailguide.android.data.security.BiometricCredentials.EmailPassword -> AuthProvider.BIOMETRIC
                        is com.trailguide.android.data.security.BiometricCredentials.SessionToken -> {
                            // Determine provider from user metadata
                            when {
                                supabaseUser.appMetadata?.get("provider")?.jsonPrimitive?.content == "google" -> AuthProvider.GOOGLE
                                else -> AuthProvider.BIOMETRIC
                            }
                        }
                    }
                )
                
                emit(NetworkResult.Success(user))
                Log.d(TAG, "Successfully signed in with biometric: ${user.email}")
            } else {
                emit(NetworkResult.Error("Biometric sign-in succeeded but session not available"))
                Log.e(TAG, "Supabase user is null after biometric sign-in")
            }
            
        } catch (e: Exception) {
            emit(NetworkResult.Error("Biometric sign-in failed: ${e.message}", e))
            Log.e(TAG, "Biometric sign-in error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clear stored biometric credentials.
     */
    fun clearBiometricCredentials() {
        biometricAuthManager.clearStoredCredentials()
        Log.d(TAG, "Biometric credentials cleared")
    }
}
