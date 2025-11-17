package com.trailguide.android.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore for user preferences including language, notifications, theme, and biometric settings.
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
        
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_WEATHER_ALERTS = booleanPreferencesKey("weather_alerts")
        private val KEY_FRIEND_ACTIVITY = booleanPreferencesKey("friend_activity")
        private val KEY_NEW_TRAILS = booleanPreferencesKey("new_trails")
        
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_AFRIKAANS = "af"
        const val LANGUAGE_ZULU = "zu"
        
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }
    
    private val dataStore = context.dataStore
    
    // Language preference
    val languageFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: LANGUAGE_ENGLISH
    }
    
    suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }
    
    // Notifications enabled
    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    // Biometric enabled
    val biometricEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }
    
    // Theme mode
    val themeModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: THEME_SYSTEM
    }
    
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }
    
    // Weather alerts
    val weatherAlertsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_WEATHER_ALERTS] ?: true
    }
    
    suspend fun setWeatherAlerts(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_WEATHER_ALERTS] = enabled
        }
    }
    
    // Friend activity notifications
    val friendActivityFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_FRIEND_ACTIVITY] ?: true
    }
    
    suspend fun setFriendActivity(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_FRIEND_ACTIVITY] = enabled
        }
    }
    
    // New trails notifications
    val newTrailsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NEW_TRAILS] ?: true
    }
    
    suspend fun setNewTrails(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NEW_TRAILS] = enabled
        }
    }
    
    /**
     * Clear all preferences.
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

