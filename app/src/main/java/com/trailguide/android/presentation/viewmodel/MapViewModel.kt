package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.osm.OsmTrail
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.repository.GoogleTrailRepository
import com.trailguide.android.data.repository.OsmTrailRepository
import com.trailguide.android.data.repository.TrailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Map screen.
 * 
 * HYBRID ARCHITECTURE:
 * - Google Places API: Discover hiking locations, trailheads, POIs
 * - OSM Overpass API: Get actual trail geometry (real hiking paths)
 * - Google Maps SDK: Visual rendering
 * - Google Directions API: Road routing to trailhead
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val trailRepository: TrailRepository,
    private val googleTrailRepository: GoogleTrailRepository,
    private val osmTrailRepository: OsmTrailRepository
) : ViewModel() {
    
    // Google Places trails (trail discoveries/POIs)
    private val _googlePlacesTrails = MutableStateFlow<List<Trail>>(emptyList())
    val googlePlacesTrails: StateFlow<List<Trail>> = _googlePlacesTrails.asStateFlow()
    
    // OSM trails (actual hiking paths with geometry)
    private val _osmTrails = MutableStateFlow<List<OsmTrail>>(emptyList())
    val osmTrails: StateFlow<List<OsmTrail>> = _osmTrails.asStateFlow()
    
    // Selected trail (from either source)
    private val _selectedGoogleTrail = MutableStateFlow<Trail?>(null)
    val selectedGoogleTrail: StateFlow<Trail?> = _selectedGoogleTrail.asStateFlow()
    
    private val _selectedOsmTrail = MutableStateFlow<OsmTrail?>(null)
    val selectedOsmTrail: StateFlow<OsmTrail?> = _selectedOsmTrail.asStateFlow()
    
    // Loading states
    private val _isLoadingGoogleTrails = MutableStateFlow(false)
    val isLoadingGoogleTrails: StateFlow<Boolean> = _isLoadingGoogleTrails.asStateFlow()
    
    private val _isLoadingOsmTrails = MutableStateFlow(false)
    val isLoadingOsmTrails: StateFlow<Boolean> = _isLoadingOsmTrails.asStateFlow()
    
    // Error states
    private val _googleError = MutableStateFlow<String?>(null)
    val googleError: StateFlow<String?> = _googleError.asStateFlow()
    
    private val _osmError = MutableStateFlow<String?>(null)
    val osmError: StateFlow<String?> = _osmError.asStateFlow()
    
    // User location
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()
    
    // Display toggles
    private val _showOsmTrails = MutableStateFlow(true)
    val showOsmTrails: StateFlow<Boolean> = _showOsmTrails.asStateFlow()
    
    private val _showGooglePlaces = MutableStateFlow(true)
    val showGooglePlaces: StateFlow<Boolean> = _showGooglePlaces.asStateFlow()
    
    /**
     * Load trails from Google Places (for discovery)
     * Shows nearby hiking locations, trailheads, POIs
     */
    fun loadGooglePlacesTrails(latitude: Double, longitude: Double, radius: Int = 5000) {
        viewModelScope.launch {
            _isLoadingGoogleTrails.value = true
            _googleError.value = null
            
            googleTrailRepository.searchHikingTrails(
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                query = "hiking trail"
            ).collect { result ->
                _isLoadingGoogleTrails.value = false
                when (result) {
                    is NetworkResult.Success -> {
                        _googlePlacesTrails.value = result.data
                        android.util.Log.d("MapViewModel", "Loaded ${result.data.size} Google Places trails")
                    }
                    is NetworkResult.Error -> {
                        _googleError.value = result.message
                        android.util.Log.e("MapViewModel", "Google Places error: ${result.message}")
                    }
                    is NetworkResult.Loading -> {
                        _isLoadingGoogleTrails.value = true
                    }
                }
            }
        }
    }
    
    /**
     * Load OSM hiking trails (for actual trail geometry)
     * Shows real hiking paths with accurate polylines
     */
    fun loadOsmTrails(latitude: Double, longitude: Double, radius: Int = 5000) {
        viewModelScope.launch {
            _isLoadingOsmTrails.value = true
            _osmError.value = null
            
            try {
                // Timeout for slow OSM queries
                val timeoutJob = launch {
                    kotlinx.coroutines.delay(30000)
                    if (_isLoadingOsmTrails.value) {
                        _isLoadingOsmTrails.value = false
                        _osmError.value = "Request timed out. Try reducing search radius."
                    }
                }
                
                osmTrailRepository.fetchTrailsNearby(
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius
                ).collect { result ->
                    timeoutJob.cancel()
                    _isLoadingOsmTrails.value = false
                    
                    when (result) {
                        is NetworkResult.Success -> {
                            _osmTrails.value = result.data
                            android.util.Log.d("MapViewModel", "Loaded ${result.data.size} OSM trails")
                        }
                        is NetworkResult.Error -> {
                            _osmError.value = result.message
                            android.util.Log.e("MapViewModel", "OSM error: ${result.message}")
                        }
                        is NetworkResult.Loading -> {
                            _isLoadingOsmTrails.value = true
                        }
                    }
                }
            } catch (e: Exception) {
                _isLoadingOsmTrails.value = false
                _osmError.value = e.message
                android.util.Log.e("MapViewModel", "OSM exception", e)
            }
        }
    }
    
    /**
     * Load BOTH Google Places and OSM trails for comprehensive coverage
     */
    fun loadAllTrails(latitude: Double, longitude: Double, radius: Int = 5000) {
        setUserLocation(LatLng(latitude, longitude))
        loadGooglePlacesTrails(latitude, longitude, radius)
        loadOsmTrails(latitude, longitude, radius)
    }
    
    /**
     * Search for specific OSM trail by name
     * Used when a trail marker is clicked to load its detailed geometry
     */
    fun searchOsmTrailByName(
        trailName: String,
        latitude: Double,
        longitude: Double,
        radius: Int = 5000
    ) {
        viewModelScope.launch {
            _isLoadingOsmTrails.value = true
            _osmError.value = null
            
            android.util.Log.d("MapViewModel", "Searching OSM for trail: '$trailName' near ($latitude, $longitude)")
            
            try {
                // Add timeout for search
                kotlinx.coroutines.withTimeout(20000) { // 20 second timeout
                    osmTrailRepository.searchTrails(
                        searchQuery = trailName,
                        latitude = latitude,
                        longitude = longitude,
                        radius = radius
                    ).collect { result ->
                        _isLoadingOsmTrails.value = false
                        when (result) {
                            is NetworkResult.Success -> {
                                if (result.data.isEmpty()) {
                                    android.util.Log.w("MapViewModel", "No OSM trails found for '$trailName'. Trail may not be in OpenStreetMap.")
                                    _osmError.value = "Trail geometry not available in OpenStreetMap"
                                } else {
                                    _osmTrails.value = result.data
                                    android.util.Log.d("MapViewModel", "Found ${result.data.size} OSM trails matching '$trailName'")
                                    
                                    // Auto-select the first/best match
                                    result.data.firstOrNull()?.let { trail ->
                                        selectOsmTrail(trail)
                                        _showOsmTrails.value = true
                                    }
                                }
                            }
                            is NetworkResult.Error -> {
                                _osmError.value = result.message
                                android.util.Log.e("MapViewModel", "Error searching OSM trails: ${result.message}")
                            }
                            is NetworkResult.Loading -> {
                                _isLoadingOsmTrails.value = true
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _isLoadingOsmTrails.value = false
                _osmError.value = "Search timed out. Trail may not exist in OpenStreetMap."
                android.util.Log.e("MapViewModel", "OSM search timed out for '$trailName'")
            } catch (e: Exception) {
                _isLoadingOsmTrails.value = false
                _osmError.value = "Search failed: ${e.message}"
                android.util.Log.e("MapViewModel", "OSM search error", e)
            }
        }
    }
    
    /**
     * General search for OSM trails (kept for backwards compatibility)
     */
    fun searchOsmTrails(query: String, latitude: Double, longitude: Double) {
        searchOsmTrailByName(query, latitude, longitude)
    }
    
    /**
     * Select a Google Places trail
     */
    fun selectGoogleTrail(trail: Trail) {
        _selectedGoogleTrail.value = trail
        _selectedOsmTrail.value = null
    }
    
    /**
     * Select an OSM trail
     */
    fun selectOsmTrail(trail: OsmTrail) {
        _selectedOsmTrail.value = trail
        _selectedGoogleTrail.value = null
    }
    
    /**
     * Clear selection
     */
    fun clearSelection() {
        _selectedGoogleTrail.value = null
        _selectedOsmTrail.value = null
    }
    
    /**
     * Toggle OSM trails visibility
     */
    fun toggleOsmTrails() {
        _showOsmTrails.value = !_showOsmTrails.value
    }
    
    /**
     * Toggle Google Places visibility
     */
    fun toggleGooglePlaces() {
        _showGooglePlaces.value = !_showGooglePlaces.value
    }
    
    /**
     * Update user location
     */
    fun setUserLocation(location: LatLng) {
        _userLocation.value = location
    }
    
    /**
     * Clear all errors
     */
    fun clearErrors() {
        _googleError.value = null
        _osmError.value = null
    }
    
    /**
     * Refresh all data
     */
    fun refresh() {
        _userLocation.value?.let { location ->
            loadAllTrails(location.latitude, location.longitude)
        }
    }
}

