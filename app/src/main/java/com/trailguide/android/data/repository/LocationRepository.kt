package com.trailguide.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user's location for weather notifications.
 * Stores last known location in DataStore.
 */
@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("location_preferences")
        
        private val LATITUDE_KEY = doublePreferencesKey("user_weather_location_lat")
        private val LONGITUDE_KEY = doublePreferencesKey("user_weather_location_lon")
        
        // Default location: Magaliesberg, South Africa (matches app's default map location)
        private const val DEFAULT_LATITUDE = -25.792
        private const val DEFAULT_LONGITUDE = 27.946
    }
    
    /**
     * Flow of user's location coordinates.
     */
    val locationFlow: Flow<Pair<Double, Double>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val lat = preferences[LATITUDE_KEY] ?: DEFAULT_LATITUDE
            val lon = preferences[LONGITUDE_KEY] ?: DEFAULT_LONGITUDE
            Pair(lat, lon)
        }
    
    /**
     * Get current location coordinates.
     * Returns default location if not set.
     */
    suspend fun getLocation(): Pair<Double, Double> {
        val preferences = context.dataStore.data.catch { emit(emptyPreferences()) }.first()
        val lat = preferences[LATITUDE_KEY] ?: DEFAULT_LATITUDE
        val lon = preferences[LONGITUDE_KEY] ?: DEFAULT_LONGITUDE
        return Pair(lat, lon)
    }
    
    /**
     * Save user's location coordinates.
     */
    suspend fun saveLocation(latitude: Double, longitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[LATITUDE_KEY] = latitude
            preferences[LONGITUDE_KEY] = longitude
        }
    }
    
    /**
     * Get default location (fallback when no location is stored).
     */
    fun getDefaultLocation(): Pair<Double, Double> {
        return Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    }
}

