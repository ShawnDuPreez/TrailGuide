package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.model.AuthProvider
import com.trailguide.android.data.model.User
import com.trailguide.android.data.repository.AuthRepository
import com.trailguide.android.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing global authentication state across the app.
 * Handles user session persistence and authentication checks.
 */
@HiltViewModel
class AuthStateViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    /**
     * Check the current authentication state on app launch.
     */
    private fun checkAuthState() {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            
            try {
                // Check if user is signed in with Supabase
                val isSignedIn = authRepository.isSignedIn()
                
                if (isSignedIn) {
                    val user = authRepository.getCurrentUserModel()
                    if (user != null) {
                        _currentUser.value = user
                        _isAuthenticated.value = true
                        
                        // User preferences will be loaded automatically via userPreferencesFlow
                    } else {
                        // Session exists but user data is invalid
                        signOut()
                    }
                } else {
                    // No active session
                    _isAuthenticated.value = false
                    _currentUser.value = null
                }
            } catch (e: Exception) {
                _authError.value = "Authentication check failed: ${e.message}"
                _isAuthenticated.value = false
                _currentUser.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Sign in the user and update authentication state.
     */
    fun signIn(user: User) {
        _currentUser.value = user
        _isAuthenticated.value = true
        _authError.value = null
        
        // User preferences are managed separately via PreferencesRepository
    }
    
    /**
     * Sign in as guest user (anonymous access).
     */
    fun signInAsGuest() {
        val guestUser = User(
            id = "guest_user",
            email = "guest@trailguide.com",
            displayName = "Guest User",
            photoUrl = null,
            provider = AuthProvider.ANONYMOUS
        )
        signIn(guestUser)
    }
    
    /**
     * Sign out the user and clear authentication state.
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut().collect { result ->
                    when (result) {
                        is com.trailguide.android.data.remote.NetworkResult.Success -> {
                            _isAuthenticated.value = false
                            _currentUser.value = null
                            _authError.value = null
                            
                            // Clear user preferences
                            preferencesRepository.clearPreferences()
                        }
                        is com.trailguide.android.data.remote.NetworkResult.Error -> {
                            _authError.value = "Sign out failed: ${result.message}"
                        }
                        is com.trailguide.android.data.remote.NetworkResult.Loading -> {
                            // Loading state
                        }
                    }
                }
            } catch (e: Exception) {
                _authError.value = "Sign out error: ${e.message}"
            }
        }
    }
    
    /**
     * Refresh authentication state (useful after OAuth callbacks).
     */
    fun refreshAuthState() {
        checkAuthState()
    }
    
    /**
     * Clear authentication error.
     */
    fun clearError() {
        _authError.value = null
    }
    
    /**
     * Check if biometric authentication is available.
     */
    fun isBiometricAvailable(): Boolean {
        return authRepository.isBiometricAvailable()
    }
    
    /**
     * Check if biometric credentials are stored.
     */
    fun hasBiometricCredentials(): Boolean {
        return authRepository.hasBiometricCredentials()
    }
}
