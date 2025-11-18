package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.google.GooglePlacesApiService
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.repository.GoogleTrailRepository
import com.trailguide.android.data.repository.TrailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Trails List screen.
 * Manages trail data, search filters, and UI state using StateFlow.
 * Now supports both backend API and Google Places API for trail discovery.
 */
@HiltViewModel
class TrailsViewModel @Inject constructor(
    private val trailRepository: TrailRepository,
    private val googleTrailRepository: GoogleTrailRepository
) : ViewModel() {
    
    companion object {
        private const val DEFAULT_RADIUS_KM = 20.0
        private val DEFAULT_LOCATION = Pair(-25.7479, 28.2293) // Pretoria CBD
    }
    
    // Toggle between backend API and Google Places
    private val _useGooglePlaces = MutableStateFlow(true)
    val useGooglePlaces: StateFlow<Boolean> = _useGooglePlaces.asStateFlow()
    
    private var currentLoadJob: Job? = null
    
    // Search and filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedDifficulty = MutableStateFlow<Difficulty?>(null)
    val selectedDifficulty: StateFlow<Difficulty?> = _selectedDifficulty.asStateFlow()
    
    private val _maxDistance = MutableStateFlow(DEFAULT_RADIUS_KM)
    val maxDistance: StateFlow<Double> = _maxDistance.asStateFlow()
    
    // Advanced filters
    private val _maxProximity = MutableStateFlow<Double?>(null) // Distance from user location in km
    val maxProximity: StateFlow<Double?> = _maxProximity.asStateFlow()
    
    private val _maxDuration = MutableStateFlow<Double?>(null) // Max duration in hours
    val maxDuration: StateFlow<Double?> = _maxDuration.asStateFlow()
    
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null) // lat, lon
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()
    
    // Trail data and loading states
    private val _trails = MutableStateFlow<List<Trail>>(emptyList())
    val trails: StateFlow<List<Trail>> = _trails.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Filtered trails based on current filters
    val filteredTrails: StateFlow<List<Trail>> = combine(
        _trails,
        _searchQuery,
        _selectedDifficulty,
        _maxDistance,
        _maxProximity,
        _maxDuration,
        _userLocation
    ) { values ->
        val trails = values[0] as List<Trail>
        val query = values[1] as String
        val difficulty = values[2] as Difficulty?
        val distance = values[3] as Double
        val proximity = values[4] as Double?
        val duration = values[5] as Double?
        val userLoc = values[6] as Pair<Double, Double>?
        
        trails.filter { trail ->
            val matchesQuery = query.isBlank() || 
                trail.name.contains(query, ignoreCase = true) ||
                (trail.city?.contains(query, ignoreCase = true) == true)
            
            val matchesDifficulty = difficulty == null || trail.difficulty == difficulty
            
            val matchesDistance = (trail.distanceKm ?: 0.0) <= distance
            
            // Proximity filter (distance from user location)
            val matchesProximity = if (proximity != null && userLoc != null) {
                val distanceFromUser = calculateDistance(
                    userLoc.first, userLoc.second,
                    trail.latitude, trail.longitude
                )
                distanceFromUser <= proximity
            } else {
                true
            }
            
            // Duration filter (estimated time to complete trail)
            val matchesDuration = if (duration != null) {
                val estimatedHours = estimateHikingDuration(trail)
                estimatedHours <= duration
            } else {
                true
            }
            
            matchesQuery && matchesDifficulty && matchesDistance && matchesProximity && matchesDuration
        }.sortedBy { it.difficulty?.order ?: 2 }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    
    /**
     * Calculate distance between two points using Haversine formula (in km)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }
    
    /**
     * Estimate hiking duration based on distance and elevation
     * Uses Naismith's rule: 1 hour per 5km + 1 hour per 600m elevation gain
     */
    private fun estimateHikingDuration(trail: Trail): Double {
        val distanceHours = (trail.distanceKm ?: 0.0) / 5.0
        val elevationHours = (trail.elevationM ?: 0) / 600.0
        return distanceHours + elevationHours
    }
    
    init {
        refresh()
    }
    
    /**
     * Load all trails from the repository.
     */
    private suspend fun loadTrailsFromApi() {
        trailRepository.getAllTrails().collect { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                    _errorMessage.value = null
                }
                is NetworkResult.Success -> {
                    _trails.value = result.data
                    _isLoading.value = false
                    _errorMessage.value = null
                }
                is NetworkResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }
            }
        }
    }
    
    private suspend fun loadTrailsFromGoogle() {
        val (lat, lon) = _userLocation.value ?: DEFAULT_LOCATION
        val radiusMeters = (_maxDistance.value * 1000).toInt().coerceIn(1000, 50000)
        val query = if (_searchQuery.value.isBlank()) {
            GooglePlacesApiService.HIKING_QUERIES.first()
        } else {
            _searchQuery.value
        }
        
        googleTrailRepository.searchHikingTrails(
            latitude = lat,
            longitude = lon,
            radius = radiusMeters,
            query = query
        ).collect { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                    _errorMessage.value = null
                }
                is NetworkResult.Success -> {
                    _trails.value = result.data
                    _isLoading.value = false
                    _errorMessage.value = if (result.data.isEmpty()) {
                        "No hiking trails found near this location."
                    } else null
                }
                is NetworkResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message ?: "Failed to fetch Google trails."
                }
            }
        }
    }
    
    private fun loadTrails(useGoogle: Boolean) {
        currentLoadJob?.cancel()
        currentLoadJob = viewModelScope.launch {
            if (useGoogle) {
                loadTrailsFromGoogle()
            } else {
                loadTrailsFromApi()
            }
        }
    }
    
    /**
     * Update search query.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (_useGooglePlaces.value) {
            refresh()
        }
    }
    
    /**
     * Update selected difficulty filter.
     */
    fun setDifficulty(difficulty: Difficulty?) {
        _selectedDifficulty.value = difficulty
        // Difficulty filter is client-side, no fetch required
    }
    
    /**
     * Update maximum distance filter.
     */
    fun setMaxDistance(distance: Double) {
        _maxDistance.value = distance
        if (_useGooglePlaces.value) {
            refresh()
        }
    }
    
    /**
     * Set user location for proximity filtering
     */
    fun setUserLocation(latitude: Double, longitude: Double) {
        _userLocation.value = Pair(latitude, longitude)
        if (_useGooglePlaces.value) {
            refresh()
        }
    }
    
    /**
     * Set maximum proximity filter (distance from user)
     */
    fun setMaxProximity(proximityKm: Double?) {
        _maxProximity.value = proximityKm
        // Only local filtering, no fetch required
    }
    
    /**
     * Set maximum duration filter
     */
    fun setMaxDuration(durationHours: Double?) {
        _maxDuration.value = durationHours
        // Local filtering only
    }
    
    /**
     * Clear all filters.
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedDifficulty.value = null
        _maxDistance.value = DEFAULT_RADIUS_KM
        _maxProximity.value = null
        _maxDuration.value = null
        
        if (_useGooglePlaces.value) {
            refresh()
        }
    }
    
    fun useGoogleTrails(enable: Boolean) {
        if (_useGooglePlaces.value == enable) return
        _useGooglePlaces.value = enable
        refresh()
    }
    
    /**
     * Toggle favorite status for a trail.
     */
    fun toggleFavorite(trail: Trail, isFavorite: Boolean) {
        viewModelScope.launch {
            trailRepository.toggleFavorite(trail, isFavorite).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // Update local trail list immediately for responsive UI
                        _trails.value = _trails.value.map { item ->
                            if (item.id == trail.id) {
                                item.copy(isFavorite = isFavorite)
                            } else {
                                item
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = "Failed to update favorite: ${result.message}"
                    }
                    is NetworkResult.Loading -> { /* No action needed */ }
                }
            }
        }
    }
    
    /**
     * Refresh the trails list
     */
    fun refresh() {
        loadTrails(_useGooglePlaces.value)
    }
    
    /**
     * Delete a trail by ID.
     */
    fun deleteTrail(trailId: String) {
        viewModelScope.launch {
            trailRepository.deleteTrail(trailId).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // Remove from local list
                        _trails.value = _trails.value.filter { it.id != trailId }
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = "Failed to delete trail: ${result.message}"
                    }
                    is NetworkResult.Loading -> { /* No action needed */ }
                }
            }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

