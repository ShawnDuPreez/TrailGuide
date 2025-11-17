package com.trailguide.android.data.repository

import android.util.Log
import com.trailguide.android.data.model.AuthProvider
import com.trailguide.android.data.model.User
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.datastore.SecureSessionStore
import com.trailguide.android.data.security.BiometricAuthenticationManager
import com.trailguide.android.data.security.BiometricStorageService
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
    private val biometricAuthManager: BiometricAuthenticationManager,
    private val biometricStorageService: BiometricStorageService,
    private val secureSessionStore: SecureSessionStore
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
                
                // Persist session for biometric if enabled (check by email)
                val normalizedEmail = email.trim().lowercase()
                val userId = supabaseUser.id
                if (biometricStorageService.isBiometricEnabled(normalizedEmail)) {
                    persistSessionForBiometrics(normalizedEmail, userId)
                }
                
                emit(NetworkResult.Success(user))
                Log.d(TAG, "Successfully signed in with email: ${user.email}")
            } else {
                emit(NetworkResult.Error("Sign-in succeeded but session not available. Try again."))
                Log.e(TAG, "Supabase user is null after sign-in")
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Invalid login credentials", ignoreCase = true) == true ||
                e.message?.contains("Invalid login", ignoreCase = true) == true ||
                e is io.github.jan.supabase.exceptions.BadRequestRestException && 
                    (e.message?.contains("Invalid", ignoreCase = true) == true ||
                     e.message?.contains("credentials", ignoreCase = true) == true) -> 
                    "Invalid email or password. If you signed up with Google, use Google sign-in instead."
                e.message?.contains("Email not confirmed", ignoreCase = true) == true ||
                e.message?.contains("not confirmed", ignoreCase = true) == true ->
                    "Please check your email and confirm your account first."
                e.message?.contains("User not found", ignoreCase = true) == true ->
                    "No account found with this email. Please sign up first."
                else -> "Sign-in failed: ${e.message ?: "Unknown error"}"
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
     * @param global If true, clears all data including biometric. If false, keeps session tokens for biometric login.
     */
    suspend fun signOut(global: Boolean = false): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            val currentUser = supabaseClient.auth.currentUserOrNull()
            val userIdToPreserve = currentUser?.id
            
            if (global) {
                // Global sign out: invalidate token on server, clear all biometric data
                if (currentUser != null) {
                    Log.d(TAG, "Signing out user globally: ${currentUser.email}")
                    supabaseClient.auth.signOut()
                }
                
                if (userIdToPreserve != null && currentUser != null) {
                    val userEmail = currentUser.email?.trim()?.lowercase()
                    if (userEmail != null) {
                        // Clear all biometric data for this email
                        biometricStorageService.clearAllUserData(userEmail)
                        biometricAuthManager.clearStoredCredentials(userIdToPreserve)
                        // Disable biometric authentication (only for global sign out)
                        biometricStorageService.setBiometricEnabled(userEmail, false)
                    }
                }
                
                emit(NetworkResult.Success(Unit))
                Log.d(TAG, "User signed out globally")
            } else {
                // Local sign out: clear Supabase session to trigger navigation back to login
                // but keep biometric tokens so biometric login still works
                if (currentUser != null) {
                    Log.d(TAG, "Signing out locally (keeping biometric tokens)")
                    supabaseClient.auth.signOut()
                }
                
                // Don't clear biometric data - tokens remain for biometric login
                // The refresh token stored in biometric storage remains valid and can be reused.
                
                emit(NetworkResult.Success(Unit))
            }
        } catch (e: Exception) {
            // Handle the specific JWT error gracefully
            if (e.message?.contains("sub claim in JWT does not exist") == true ||
                e.message?.contains("Invalid Refresh Token") == true) {
                Log.w(TAG, "Session already invalid, clearing local state")
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error("Sign-out failed: ${e.message}", e))
                Log.e(TAG, "Sign-out error", e)
            }
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
        val userId = currentUser?.id ?: return false
        return biometricAuthManager.hasStoredCredentials(userId)
    }
    
    /**
     * Check if biometric is enabled for a specific email (for login screen).
     * Simplified - checks directly by email, no userId lookup needed.
     */
    suspend fun hasBiometricForEmail(email: String): Boolean {
        if (!biometricAuthManager.canUseBiometric()) {
            return false
        }
        
        val normalizedEmail = email.trim().lowercase()
        val hasCredentials = biometricStorageService.hasCredentialsForEmail(normalizedEmail)
        if (!hasCredentials) {
            return false
        }
        
        // Check if biometric is enabled for this email
        return biometricStorageService.isBiometricEnabled(normalizedEmail)
    }
    
    /**
     * Store user credentials securely using biometric authentication.
     * @param activity FragmentActivity needed for biometric prompt
     * @param email User's email
     * @param password User's password
     * @return Flow with success/error result
     */
    suspend fun storeBiometricCredentials(activity: androidx.fragment.app.FragmentActivity, email: String, password: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            if (!biometricAuthManager.canUseBiometric()) {
                emit(NetworkResult.Error("Biometric authentication is not available on this device"))
                return@flow
            }
            
            val userId = currentUser?.id
            if (userId == null) {
                emit(NetworkResult.Error("User not authenticated"))
                return@flow
            }
            
            val normalizedEmail = email.trim().lowercase()
            
            // Migrate old credentials if needed
            biometricStorageService.migrateOldCredentials(normalizedEmail)
            
            // Store credentials using biometric manager (for password fallback)
            // Note: Still need userId for Android Keystore key alias
            val success = biometricAuthManager.storeCredentialsWithBiometric(activity, userId, email, password)
            if (!success) {
                emit(NetworkResult.Error("Failed to store biometric credentials"))
                return@flow
            }
            
            // Get refresh token from SecureSessionStore if available
            val refreshToken = secureSessionStore.getRefreshToken()
            
            // Save to BiometricStorageService by email (simplified - no userId lookup needed)
            biometricStorageService.saveCredentials(
                email = normalizedEmail,
                refreshToken = refreshToken,
                sessionData = null, // Will be populated after login
                password = password,
                userId = userId
            )
            
            // Enable biometric for this email (state persists across logouts)
            biometricStorageService.setBiometricEnabled(normalizedEmail, true)
            
            emit(NetworkResult.Success(Unit))
            Log.d(TAG, "Biometric credentials stored successfully for user $userId")
        } catch (e: Exception) {
            emit(NetworkResult.Error("Failed to store biometric credentials: ${e.message}", e))
            Log.e(TAG, "Error storing biometric credentials", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Sign in using biometric authentication.
     * This retrieves stored credentials and signs in with Supabase.
     * @param activity FragmentActivity needed for biometric prompt
     * @param email Optional email for email-based lookup
     * @return Flow with User result
     */
    suspend fun signInWithBiometric(
        activity: androidx.fragment.app.FragmentActivity,
        email: String? = null
    ): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            if (!biometricAuthManager.canUseBiometric()) {
                emit(NetworkResult.Error("Biometric authentication is not available"))
                return@flow
            }
            
            // Get credentials by email (simplified - always use email)
            val lookupEmail: String = if (email != null) {
                email.trim().lowercase()
            } else {
                // If no email provided, try to get from current user
                currentUser?.email?.trim()?.lowercase() ?: run {
                    emit(NetworkResult.Error("No saved credentials found. Please sign in with your password."))
                    return@flow
                }
            }
            
            val credentials = biometricStorageService.getCredentialsByEmail(lookupEmail)
            if (credentials == null) {
                emit(NetworkResult.Error("No saved credentials found. Please sign in with your password."))
                return@flow
            }
            
            // Get userId from credentials or current user
            val resolvedUserId = credentials["userId"] ?: currentUser?.id ?: run {
                emit(NetworkResult.Error("No user ID found. Please sign in with your password."))
                return@flow
            }
            
            // Authenticate with biometric
            val localizedReason = if (email != null) {
                "Authenticate as $lookupEmail"
            } else {
                "Authenticate to access your account"
            }
            
            val authResult = biometricAuthManager.authenticateWithBiometric(
                activity = activity,
                title = "Sign in to TrailGuide",
                subtitle = localizedReason
            )
            
            if (!authResult) {
                emit(NetworkResult.Error("Biometric authentication failed"))
                return@flow
            }
            
            // Try refresh token first (if available)
            // Note: Supabase Kotlin SDK doesn't have direct setSession method
            // We'll skip refresh token for now and go straight to password fallback
            // Refresh token can be used in future if Supabase SDK adds this capability
            
            // Fallback to password
            val password = credentials["password"]
            if (!password.isNullOrEmpty()) {
                // Retrieve password from biometric manager (encrypted)
                val keystoreCredentials = biometricAuthManager.retrieveCredentialsWithBiometric(activity, resolvedUserId)
                if (keystoreCredentials != null) {
                    val (_, keystorePassword) = keystoreCredentials
                    
                    supabaseClient.auth.signInWith(Email) {
                        this.email = lookupEmail
                        this.password = keystorePassword
                    }
                    
                    kotlinx.coroutines.delay(300)
                    
                    val supabaseUser = supabaseClient.auth.currentUserOrNull()
                    if (supabaseUser != null) {
                        val user = User(
                            id = supabaseUser.id,
                            email = supabaseUser.email ?: lookupEmail,
                            displayName = supabaseUser.userMetadata?.get("full_name")?.jsonPrimitive?.content
                                ?: supabaseUser.userMetadata?.get("display_name")?.jsonPrimitive?.content
                                ?: lookupEmail.substringBefore("@"),
                            photoUrl = supabaseUser.userMetadata?.get("avatar_url")?.jsonPrimitive?.content,
                            provider = AuthProvider.BIOMETRIC
                        )
                        
                        // Persist session after successful login (using email, not userId)
                        persistSessionForBiometrics(lookupEmail, supabaseUser.id)
                        
                        emit(NetworkResult.Success(user))
                        Log.d(TAG, "Successfully signed in with biometric (password fallback): ${user.email}")
                        return@flow
                    }
                }
            }
            
            emit(NetworkResult.Error("Session expired. Please enter your password to sign in."))
            
        } catch (e: Exception) {
            emit(NetworkResult.Error("Biometric sign-in failed: ${e.message}", e))
            Log.e(TAG, "Biometric sign-in error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Persist session for biometric authentication after login/refresh.
     * Uses email-based storage (simplified).
     */
    private suspend fun persistSessionForBiometrics(email: String, userId: String?) {
        val currentUser = supabaseClient.auth.currentUserOrNull() ?: return
        val resolvedUserId = userId ?: currentUser.id
        val normalizedEmail = email.trim().lowercase()
        
        // Get refresh token from SecureSessionStore
        val refreshToken = secureSessionStore.getRefreshToken()
        
        // Build session data JSON (simplified - just essential info)
        val sessionData = try {
            buildJsonObject {
                put("user_id", resolvedUserId)
                put("email", normalizedEmail)
                if (refreshToken != null) {
                    put("refresh_token", refreshToken)
                }
            }.toString()
        } catch (e: Exception) {
            null
        }
        
        // Save credentials by email (simplified - no userId lookup needed)
        biometricStorageService.saveCredentials(
            email = normalizedEmail,
            refreshToken = refreshToken,
            sessionData = sessionData,
            userId = resolvedUserId
        )
        // Note: Biometric enabled state persists - don't change it here
    }
    
    /**
     * Clear stored biometric credentials for current user.
     */
    suspend fun clearBiometricCredentials() {
        val currentUser = currentUser
        val userEmail = currentUser?.email?.trim()?.lowercase()
        val userId = currentUser?.id
        
        if (userEmail != null && userId != null) {
            biometricAuthManager.clearStoredCredentials(userId)
            biometricStorageService.clearAllUserData(userEmail)
            Log.d(TAG, "Biometric credentials cleared for email $userEmail")
        }
    }
    
    /**
     * Clear biometric data for a specific email.
     */
    suspend fun clearBiometricData(email: String, userId: String) {
        val normalizedEmail = email.trim().lowercase()
        biometricAuthManager.clearStoredCredentials(userId)
        biometricStorageService.clearAllUserData(normalizedEmail)
        Log.d(TAG, "Biometric data cleared for email $normalizedEmail")
    }
}
