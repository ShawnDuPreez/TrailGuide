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
    
    private val _biometricGatePassed = MutableStateFlow(false)
    val biometricGatePassed: StateFlow<Boolean> = _biometricGatePassed.asStateFlow()
    
    // User preferences flow
    val userPreferences: StateFlow<com.trailguide.android.data.model.UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.trailguide.android.data.model.UserPreferences()
        )
    
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
                        
                        // Reset biometric gate on app start - user must authenticate again
                        _biometricGatePassed.value = false
                        
                        // User preferences will be loaded automatically via userPreferencesFlow
                    } else {
                        // Session exists but user data is invalid - clear state
                        _isAuthenticated.value = false
                        _currentUser.value = null
                        _biometricGatePassed.value = false
                    }
                } else {
                    // No active session - ensure we're signed out
                    _isAuthenticated.value = false
                    _currentUser.value = null
                    _biometricGatePassed.value = false
                }
            } catch (e: Exception) {
                _authError.value = "Authentication check failed: ${e.message}"
                _isAuthenticated.value = false
                _currentUser.value = null
                _biometricGatePassed.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Sign in the user and update authentication state.
     * Resets biometric gate so user must authenticate if biometric is enabled.
     */
    fun signIn(user: User) {
        _currentUser.value = user
        _isAuthenticated.value = true
        _authError.value = null
        _biometricGatePassed.value = false // Reset biometric gate on new sign in
        
        // User preferences are managed separately via PreferencesRepository
    }
    
    /**
     * Sign out the user and clear authentication state.
     * Always clears local state immediately, even if API call fails.
     * Reference: Developers, G., 2025. State and Jetpack Compose. https://developer.android.com/develop/ui/compose/state
     */
    fun signOut() {
        // Clear state immediately to ensure logout happens right away
        // This ensures the UI updates immediately and user is redirected to login
        // Reference: Kumar, M., 2024. Jetpack Compose : State Management. https://medium.com/@manishkumar_75473/jetpack-compose-state-management-part-1-7d2b4d980455
        _isAuthenticated.value = false
        _currentUser.value = null
        _authError.value = null
        _biometricGatePassed.value = false
        _isLoading.value = false
        
        // Also try to sign out from Supabase in the background (non-blocking)
        // This doesn't block the UI update since state is already cleared above
        viewModelScope.launch {
            try {
                // Just collect once - state is already cleared so this is just cleanup
                authRepository.signOut().collect { result ->
                    // State is already cleared above, just log any errors for debugging
                    if (result is com.trailguide.android.data.remote.NetworkResult.Error) {
                        android.util.Log.w("AuthStateViewModel", "Sign out API error: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                // Log exception but state is already cleared
                android.util.Log.w("AuthStateViewModel", "Sign out exception: ${e.message}")
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
    
    /**
     * Mark biometric gate as passed (after successful biometric authentication).
     */
    fun passBiometricGate() {
        _biometricGatePassed.value = true
    }
    
    /**
     * Reset biometric gate (when user signs out or app restarts).
     */
    fun resetBiometricGate() {
        _biometricGatePassed.value = false
    }
}
