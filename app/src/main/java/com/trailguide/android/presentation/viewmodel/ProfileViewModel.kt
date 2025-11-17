package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.model.Language
import com.trailguide.android.data.model.User
import com.trailguide.android.data.model.UserPreferences
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.repository.AuthRepository
import com.trailguide.android.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Profile/Settings screen.
 * Manages user authentication state with Supabase and preferences.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    // User authentication state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()
    
    // User preferences
    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )
    
    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    /**
     * Check current authentication state.
     */
    private fun checkAuthState() {
        _isSignedIn.value = authRepository.isSignedIn()
        _currentUser.value = authRepository.getCurrentUserModel()
    }
    
    /**
     * Sign in with Google using Supabase OAuth.
     */
    fun signInWithGoogle() {
        viewModelScope.launch {
            authRepository.signInWithGoogle().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        // If we got a placeholder (email is "Redirecting..."), 
                        // the OAuth flow is starting - keep loading state
                        if (result.data.email == "Redirecting...") {
                            _isLoading.value = true
                            _successMessage.value = "Opening browser for Google sign-in..."
                            
                            // Schedule multiple refresh attempts after OAuth callback
                            viewModelScope.launch {
                                // Try refreshing after increasing delays to catch the callback
                                kotlinx.coroutines.delay(1000)
                                refreshAuthState()
                                
                                kotlinx.coroutines.delay(1000)
                                refreshAuthState()
                                
                                kotlinx.coroutines.delay(2000)
                                refreshAuthState()
                                
                                // Final check after 5 seconds total
                                kotlinx.coroutines.delay(1000)
                                if (!_isSignedIn.value) {
                                    _isLoading.value = false
                                    _errorMessage.value = "Sign-in timed out. Please try again."
                                }
                            }
                        } else {
                            // Got actual user data
                            _currentUser.value = result.data
                            _isSignedIn.value = true
                            _isLoading.value = false
                            _successMessage.value = "Signed in successfully with Google!"
                        }
                    }
                    is NetworkResult.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = result.message
                    }
                }
            }
        }
    }
    
    /**
     * Refresh authentication state from repository.
     */
    fun refreshAuthState() {
        val isSignedIn = authRepository.isSignedIn()
        val currentUser = authRepository.getCurrentUserModel()
        
        if (isSignedIn && currentUser != null) {
            _isSignedIn.value = true
            _currentUser.value = currentUser
            _isLoading.value = false
            _successMessage.value = "Welcome back, ${currentUser.displayName ?: currentUser.email}!"
        } else if (!isSignedIn && _currentUser.value?.email != "Redirecting...") {
            // Only clear user if we're not in the middle of OAuth flow
            _isSignedIn.value = false
            _currentUser.value = null
        }
    }
    
    /**
     * Sign in with email and password using Supabase Auth.
     */
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signInWithEmail(email, password).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _currentUser.value = result.data
                        _isSignedIn.value = true
                        _isLoading.value = false
                        _successMessage.value = "Signed in successfully!"
                    }
                    is NetworkResult.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = result.message
                    }
                }
            }
        }
    }
    
    /**
     * Register new user with email and password.
     */
    fun registerWithEmail(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            authRepository.registerWithEmail(email, password, displayName).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _currentUser.value = result.data
                        _isSignedIn.value = true
                        _isLoading.value = false
                        _successMessage.value = "Account created successfully!"
                    }
                    is NetworkResult.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = result.message
                    }
                }
            }
        }
    }
    
    /**
     * Sign out current user from Supabase.
     * This clears the session and will trigger AuthWrapper to show login screen.
     * Uses global=false to keep biometric tokens, but clears local session.
     */
    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            // Immediately clear local state to prevent flashing
            _currentUser.value = null
            _isSignedIn.value = false
            
            authRepository.signOut(global = false).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                    }
                    is NetworkResult.Success -> {
                        _isLoading.value = false
                        _successMessage.value = "Signed out successfully!"
                        // Clear preferences
                        preferencesRepository.clearPreferences()
                        // Note: AuthStateViewModel will be updated via checkAuthState() 
                        // which is called when AuthWrapper refreshes
                    }
                    is NetworkResult.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = result.message
                    }
                }
            }
        }
    }
    
    /**
     * Update language preference.
     */
    fun setLanguage(language: Language) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }
    
    /**
     * Update biometrics enabled preference.
     */
    fun setBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricsEnabled(enabled)
        }
    }
    
    /**
     * Update notifications enabled preference.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clear success message.
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
