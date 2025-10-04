package com.trailguide.android.presentation.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.trailguide.android.data.model.RoutePoint
import com.trailguide.android.data.model.Trail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*
import javax.inject.Inject

/**
 * ViewModel for the Hiking screen.
 * Manages GPS tracking, timer, elevation, and progress calculations.
 */
@HiltViewModel
class HikingViewModel @Inject constructor() : ViewModel() {
    
    // Hiking state
    private val _isHiking = MutableStateFlow(false)
    val isHiking: StateFlow<Boolean> = _isHiking.asStateFlow()
    
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    
    // Timer
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()
    
    private var startTime: Long = 0L
    private var pausedTime: Long = 0L
    private var timerJob: Job? = null
    
    // Location tracking
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()
    
    private val _currentElevation = MutableStateFlow(0.0)
    val currentElevation: StateFlow<Double> = _currentElevation.asStateFlow()
    
    // Distance and progress
    private val _distanceTraveled = MutableStateFlow(0.0)
    val distanceTraveled: StateFlow<Double> = _distanceTraveled.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    // Trail data
    private var currentTrail: Trail? = null
    private var lastKnownLocation: LatLng? = null
    private var totalTrailDistance: Double = 0.0
    
    // Location manager
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    
    /**
     * Request location permission and start GPS tracking.
     */
    fun requestLocationPermission(context: Context) {
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startLocationTracking(context)
            } else {
                // Permission not granted - this is expected on first launch
                // The user will need to grant permission manually
            }
        } catch (e: Exception) {
            // Handle any errors gracefully
        }
    }
    
    /**
     * Start GPS location tracking.
     */
    private fun startLocationTracking(context: Context) {
        try {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try {
                        val latLng = LatLng(location.latitude, location.longitude)
                        _currentLocation.value = latLng
                        _currentElevation.value = location.altitude
                        
                        // Calculate distance traveled
                        lastKnownLocation?.let { lastLocation ->
                            val distance = calculateDistance(lastLocation, latLng)
                            if (_isHiking.value && !_isPaused.value) {
                                _distanceTraveled.value += distance
                                updateProgress()
                            }
                        }
                        
                        lastKnownLocation = latLng
                    } catch (e: Exception) {
                        // Handle location processing errors
                    }
                }
                
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            
            // Check if GPS is available
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L, // 2 seconds (less frequent to avoid crashes)
                    5f, // 5 meters (less precise to avoid crashes)
                    locationListener!!
                )
            }
        } catch (e: SecurityException) {
            // Permission not granted - this is expected
        } catch (e: Exception) {
            // Handle any other errors
        }
    }
    
    /**
     * Set the current trail for tracking.
     */
    fun setTrail(trail: Trail) {
        currentTrail = trail
        totalTrailDistance = calculateTrailDistance(trail.routeCoordinates)
    }
    
    /**
     * Start hiking.
     */
    fun startHike() {
        _isHiking.value = true
        _isPaused.value = false
        startTime = System.currentTimeMillis()
        pausedTime = 0L
        
        startTimer()
    }
    
    /**
     * Pause hiking.
     */
    fun pauseHike() {
        _isPaused.value = true
        timerJob?.cancel()
    }
    
    /**
     * Resume hiking.
     */
    fun resumeHike() {
        _isPaused.value = false
        pausedTime += System.currentTimeMillis() - startTime
        startTimer()
    }
    
    /**
     * Stop hiking.
     */
    fun stopHike() {
        _isHiking.value = false
        _isPaused.value = false
        timerJob?.cancel()
        
        // Reset values
        _elapsedTime.value = 0L
        _distanceTraveled.value = 0.0
        _progress.value = 0f
        startTime = 0L
        pausedTime = 0L
    }
    
    /**
     * Start the timer.
     */
    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_isHiking.value && !_isPaused.value) {
                _elapsedTime.value = System.currentTimeMillis() - startTime - pausedTime
                delay(1000) // Update every second
            }
        }
    }
    
    /**
     * Update progress based on distance traveled.
     */
    private fun updateProgress() {
        currentTrail?.let { trail ->
            if (totalTrailDistance > 0) {
                val progressValue = (_distanceTraveled.value / totalTrailDistance).coerceIn(0.0, 1.0).toFloat()
                _progress.value = progressValue
            }
        }
    }
    
    /**
     * Calculate distance between two points in meters.
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters
        
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLngRad = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = sin(deltaLatRad / 2.0) * sin(deltaLatRad / 2.0) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLngRad / 2.0) * sin(deltaLngRad / 2.0)
        
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        
        return earthRadius * c
    }
    
    /**
     * Calculate total trail distance from route coordinates.
     */
    private fun calculateTrailDistance(routePoints: List<RoutePoint>): Double {
        if (routePoints.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 1 until routePoints.size) {
            val point1 = routePoints[i - 1]
            val point2 = routePoints[i]
            totalDistance += calculateDistance(
                LatLng(point1.latitude, point1.longitude),
                LatLng(point2.latitude, point2.longitude)
            )
        }
        
        return totalDistance
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
        }
    }
}
