package com.trailguide.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.trailguide.android.data.model.AppTheme
import com.trailguide.android.data.model.Language
import com.trailguide.android.data.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user preferences using DataStore.
 * Provides a reactive way to read and write settings.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_preferences")
        
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val BIOMETRICS_ENABLED_KEY = booleanPreferencesKey("biometrics_enabled")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val NOTIFICATION_TIME_HOUR_KEY = intPreferencesKey("notification_time_hour")
        private val NOTIFICATION_TIME_MINUTE_KEY = intPreferencesKey("notification_time_minute")
        private val OFFLINE_MAPS_ENABLED_KEY = booleanPreferencesKey("offline_maps_enabled")
        private val THEME_KEY = stringPreferencesKey("theme")
        
        // Default notification time: 6 AM
        private const val DEFAULT_NOTIFICATION_HOUR = 6
        private const val DEFAULT_NOTIFICATION_MINUTE = 0
    }
    
    /**
     * Flow of user preferences.
     */
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                language = Language.fromCode(preferences[LANGUAGE_KEY] ?: "en"),
                biometricsEnabled = preferences[BIOMETRICS_ENABLED_KEY] ?: false,
                notificationsEnabled = preferences[NOTIFICATIONS_ENABLED_KEY] ?: true,
                offlineMapsEnabled = preferences[OFFLINE_MAPS_ENABLED_KEY] ?: false,
                theme = AppTheme.valueOf(preferences[THEME_KEY] ?: "SYSTEM")
            )
        }
    
    /**
     * Update language preference.
     */
    suspend fun setLanguage(language: Language) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }
        // Also update SharedPreferences cache for synchronous access in attachBaseContext
        val sharedPrefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("language", language.code).apply()
    }
    
    /**
     * Update biometrics enabled preference.
     */
    suspend fun setBiometricsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRICS_ENABLED_KEY] = enabled
        }
    }
    
    /**
     * Update notifications enabled preference.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }
    
    /**
     * Get notification time as Flow.
     * Returns (hour, minute) pair with defaults (6, 0) if not set.
     */
    val notificationTimeFlow: Flow<Pair<Int, Int>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val hour = preferences[NOTIFICATION_TIME_HOUR_KEY] ?: DEFAULT_NOTIFICATION_HOUR
            val minute = preferences[NOTIFICATION_TIME_MINUTE_KEY] ?: DEFAULT_NOTIFICATION_MINUTE
            Pair(hour, minute)
        }
    
    /**
     * Get current notification time.
     * Returns default (6, 0) if not set.
     */
    suspend fun getNotificationTime(): Pair<Int, Int> {
        val preferences = context.dataStore.data.catch { emit(emptyPreferences()) }.first()
        val hour = preferences[NOTIFICATION_TIME_HOUR_KEY] ?: DEFAULT_NOTIFICATION_HOUR
        val minute = preferences[NOTIFICATION_TIME_MINUTE_KEY] ?: DEFAULT_NOTIFICATION_MINUTE
        return Pair(hour, minute)
    }
    
    /**
     * Set notification time.
     */
    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_TIME_HOUR_KEY] = hour
            preferences[NOTIFICATION_TIME_MINUTE_KEY] = minute
        }
    }
    
    /**
     * Update offline maps enabled preference.
     */
    suspend fun setOfflineMapsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OFFLINE_MAPS_ENABLED_KEY] = enabled
        }
    }
    
    /**
     * Update app theme preference.
     */
    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
    
    /**
     * Clear all preferences.
     */
    suspend fun clearPreferences() {
        context.dataStore.edit { it.clear() }
    }
}

