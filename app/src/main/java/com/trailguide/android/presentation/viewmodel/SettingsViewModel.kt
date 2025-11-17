package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.BuildConfig
import com.trailguide.android.data.datastore.SecureSessionStore
import com.trailguide.android.data.datastore.UserPreferences
import com.trailguide.android.data.repository.AuthRepository
import com.trailguide.android.data.security.BiometricAuthenticationManager
import com.trailguide.android.data.security.BiometricStorageService
import com.trailguide.android.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen.
 * Manages user preferences, biometric authentication, sync, and app settings.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val secureSessionStore: SecureSessionStore,
    private val authRepository: AuthRepository,
    private val biometricAuthManager: BiometricAuthenticationManager,
    private val biometricStorageService: BiometricStorageService,
    private val syncScheduler: SyncScheduler
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Preferences flows
    val languageFlow = userPreferences.languageFlow
    val notificationsEnabledFlow = userPreferences.notificationsEnabledFlow
    val weatherAlertsFlow = userPreferences.weatherAlertsFlow
    val friendActivityFlow = userPreferences.friendActivityFlow
    val newTrailsFlow = userPreferences.newTrailsFlow
    val themeModeFlow = userPreferences.themeModeFlow
    
    // Biometric enabled flow - based on email (persists across logouts)
    val biometricEnabledFlow: StateFlow<Boolean> = flow {
        while (true) {
            val currentUser = authRepository.currentUser
            val email = currentUser?.email?.trim()?.lowercase()
            if (email != null) {
                emit(biometricStorageService.isBiometricEnabled(email))
            } else {
                emit(false)
            }
            kotlinx.coroutines.delay(1000) // Check every second
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    init {
        checkBiometricAvailability()
        observeSyncStatus()
    }
    
    /**
     * Check if biometric authentication is available on device.
     */
    private fun checkBiometricAvailability() {
        val isAvailable = biometricAuthManager.canUseBiometric()
        _uiState.update { it.copy(biometricAvailable = isAvailable) }
    }
    
    /**
     * Observe sync status from WorkManager.
     */
    private fun observeSyncStatus() {
        viewModelScope.launch {
            // Check sync status periodically
            val isSyncing = syncScheduler.isSyncRunning()
            _uiState.update { it.copy(isSyncing = isSyncing) }
        }
    }
    
    /**
     * Set app language.
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            userPreferences.setLanguage(languageCode)
        }
    }
    
    /**
     * Toggle notifications.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNotificationsEnabled(enabled)
        }
    }
    
    /**
     * Toggle biometric authentication.
     * Uses email-based storage (persists across logouts).
     */
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            val email = currentUser?.email?.trim()?.lowercase()
            
            if (email != null) {
                // Set biometric enabled state by email (persists across logouts)
                biometricStorageService.setBiometricEnabled(email, enabled)
                
                // Also update UserPreferences for UI consistency
                userPreferences.setBiometricEnabled(enabled)
                secureSessionStore.setBiometricEnabled(enabled)
                
                if (!enabled) {
                    // Clear biometric credentials when disabled
                    authRepository.clearBiometricCredentials()
                }
            }
        }
    }
    
    /**
     * Toggle weather alerts.
     */
    fun setWeatherAlerts(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setWeatherAlerts(enabled)
        }
    }
    
    /**
     * Toggle friend activity notifications.
     */
    fun setFriendActivity(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setFriendActivity(enabled)
        }
    }
    
    /**
     * Toggle new trails notifications.
     */
    fun setNewTrails(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNewTrails(enabled)
        }
    }
    
    /**
     * Set theme mode.
     */
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }
    
    /**
     * Trigger manual sync.
     */
    fun triggerManualSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = "Syncing...") }
            
            try {
                syncScheduler.scheduleOneTimeSync()
                
                // Wait a bit for sync to start
                kotlinx.coroutines.delay(2000)
                
                _uiState.update { 
                    it.copy(
                        isSyncing = false, 
                        syncMessage = "Sync completed",
                        lastSyncTime = System.currentTimeMillis()
                    ) 
                }
                
                // Clear message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(syncMessage = null) }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSyncing = false, 
                        syncMessage = "Sync failed: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Get app version.
     */
    fun getAppVersion(): String {
        return "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }
    
    /**
     * Check if biometric is available.
     */
    fun isBiometricAvailable(): Boolean {
        return biometricAuthManager.canUseBiometric()
    }
}

/**
 * UI state for Settings screen.
 */
data class SettingsUiState(
    val biometricAvailable: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val lastSyncTime: Long? = null
)

