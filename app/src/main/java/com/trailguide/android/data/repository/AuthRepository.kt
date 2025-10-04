package com.trailguide.android.data.repository

import android.util.Log
import com.trailguide.android.data.model.AuthProvider
import com.trailguide.android.data.model.User
import com.trailguide.android.data.remote.NetworkResult
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
    private val supabaseClient: SupabaseClient
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
     */
    suspend fun signOut(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            // Check if user is actually signed in before attempting sign out
            val currentUser = supabaseClient.auth.currentUserOrNull()
            
            if (currentUser != null) {
                Log.d(TAG, "Signing out user: ${currentUser.email}")
                supabaseClient.auth.signOut()
                emit(NetworkResult.Success(Unit))
                Log.d(TAG, "User signed out successfully")
            } else {
                Log.w(TAG, "No user session found, clearing local state")
                // No active session, but clear any local state
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
}
