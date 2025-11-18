package com.trailguide.android.presentation.viewmodel

import android.content.Context
import android.content.res.Configuration as AndroidConfiguration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.model.Language
import com.trailguide.android.data.model.User
import com.trailguide.android.data.model.UserPreferences
import com.trailguide.android.data.notification.NotificationScheduler
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.repository.AuthRepository
import com.trailguide.android.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the Profile/Settings screen.
 * Manages user authentication state with Supabase and preferences.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationScheduler: NotificationScheduler,
    @ApplicationContext private val context: Context
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
    
    // Notification time state
    val notificationTime: StateFlow<Pair<Int, Int>> = preferencesRepository.notificationTimeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Pair(6, 0)
        )
    
    // Time picker dialog state
    private val _showTimePickerDialog = MutableStateFlow(false)
    val showTimePickerDialog: StateFlow<Boolean> = _showTimePickerDialog.asStateFlow()
    
    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        checkAuthState()
        // Reschedule notifications if they were previously enabled
        // (WorkManager tasks don't survive app uninstall/reinstall)
        viewModelScope.launch {
            userPreferences.first().let { preferences ->
                if (preferences.notificationsEnabled) {
                    val (hour, minute) = preferencesRepository.getNotificationTime()
                    notificationScheduler.scheduleDailyNotifications(hour, minute)
                }
            }
        }
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
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                    }
                    is NetworkResult.Success -> {
                        _currentUser.value = null
                        _isSignedIn.value = false
                        _isLoading.value = false
                        _successMessage.value = "Signed out successfully!"
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
    fun setLanguage(language: Language, onLanguageChanged: (() -> Unit)? = null) {
        viewModelScope.launch {
            // Only update if language actually changed
            val currentLanguage = userPreferences.value.language
            if (currentLanguage != language) {
                // Save preference first
                preferencesRepository.setLanguage(language)
                // Update app locale immediately
                updateAppLocale(language)
                // Wait a moment for preference to be saved, then recreate activity
                kotlinx.coroutines.delay(50)
                // Invoke callback on main thread to recreate activity
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onLanguageChanged?.invoke()
                }
            }
        }
    }
    
    /**
     * Update app locale when language changes.
     */
    private fun updateAppLocale(language: Language) {
        val locale = when (language) {
            Language.ENGLISH -> Locale("en")
            Language.AFRIKAANS -> Locale("af")
            Language.ZULU -> Locale("zu")
        }
        
        Locale.setDefault(locale)
        val config = AndroidConfiguration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
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
     * If enabling, shows time picker dialog for user to select time.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                // Show time picker dialog - user will confirm time in dialog
                // Don't save enabled state yet - wait for time selection
                showTimePicker()
            } else {
                // Disable immediately
                preferencesRepository.setNotificationsEnabled(false)
                notificationScheduler.cancelDailyNotifications()
            }
        }
    }
    
    /**
     * Enable notifications with selected time.
     * Called from time picker dialog when user confirms time.
     */
    fun enableNotificationsWithTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            // Save time and enable notifications
            preferencesRepository.setNotificationTime(hour, minute)
            preferencesRepository.setNotificationsEnabled(true)
            
            // Schedule notifications with selected time
            notificationScheduler.scheduleDailyNotifications(hour, minute)
            
            // Hide time picker
            hideTimePicker()
        }
    }
    
    /**
     * Show time picker dialog.
     */
    fun showTimePicker() {
        _showTimePickerDialog.value = true
    }
    
    /**
     * Hide time picker dialog.
     */
    fun hideTimePicker() {
        _showTimePickerDialog.value = false
    }
    
    /**
     * Set notification time and reschedule notifications.
     */
    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            // Save time to DataStore
            preferencesRepository.setNotificationTime(hour, minute)
            
            // If notifications are enabled, reschedule with new time
            val preferences = userPreferences.value
            if (preferences.notificationsEnabled) {
                notificationScheduler.scheduleDailyNotifications(hour, minute)
            }
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
    
    /**
     * Test notification immediately (for testing).
     */
    fun testNotificationNow() {
        notificationScheduler.testNotificationNow()
    }
}
