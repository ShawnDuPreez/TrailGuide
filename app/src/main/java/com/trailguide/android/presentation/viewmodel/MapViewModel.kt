package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.model.CombinedTrailStats
import com.trailguide.android.data.osm.OsmTrail
import com.trailguide.android.data.osm.OsmBoundary
import com.trailguide.android.data.osm.BoundingBox
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
    private val osmTrailRepository: OsmTrailRepository,
    private val offlineMapRepository: com.trailguide.android.data.repository.OfflineMapRepository
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
    
    // Combined trail statistics
    private val _combinedTrailStats = MutableStateFlow<CombinedTrailStats?>(null)
    val combinedTrailStats: StateFlow<CombinedTrailStats?> = _combinedTrailStats.asStateFlow()

    // Display toggles
    private val _showOsmTrails = MutableStateFlow(true)
    val showOsmTrails: StateFlow<Boolean> = _showOsmTrails.asStateFlow()
    
    private val _showGooglePlaces = MutableStateFlow(true)
    val showGooglePlaces: StateFlow<Boolean> = _showGooglePlaces.asStateFlow()
    
    /**
     * Load trails from Google Places (for discovery)
     * Shows nearby hiking locations, trailheads, POIs
     * FILTERED: Only shows major trailheads within 2x2km, limits to 10 results
     */
    fun loadGooglePlacesTrails(latitude: Double, longitude: Double, radius: Int = 2000) {
        viewModelScope.launch {
            _isLoadingGoogleTrails.value = true
            _googleError.value = null
            
            googleTrailRepository.searchHikingTrails(
                latitude = latitude,
                longitude = longitude,
                radius = radius, // Default 2km radius
                query = "hiking trail"
            ).collect { result ->
                _isLoadingGoogleTrails.value = false
                when (result) {
                    is NetworkResult.Success -> {
                        // Filter and limit results
                        val filteredTrails = result.data
                            .filter { trail ->
                                // Calculate distance from center
                                val distance = calculateDistance(
                                    LatLng(latitude, longitude),
                                    LatLng(trail.latitude, trail.longitude)
                                )
                                // Only show trails within 2km
                                distance <= 2000
                            }
                            .sortedByDescending { trail ->
                                // Prioritize trails with:
                                // 1. Higher ratings
                                // 2. More reviews (indicates popularity)
                                (trail.rating ?: 0.0) * (trail.reviewCount ?: 1)
                            }
                            .take(10) // Limit to 10 most relevant results
                        
                        _googlePlacesTrails.value = filteredTrails
                        android.util.Log.d("MapViewModel", "Filtered to ${filteredTrails.size} major trails (from ${result.data.size} total)")
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
                            updateCombinedStats(result.data)
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
     * Optimized for 2x2km area with limited results
     */
    fun loadAllTrails(latitude: Double, longitude: Double, radius: Int = 2000) {
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
                // Add timeout for search - OSM is supplementary so fail fast
                kotlinx.coroutines.withTimeout(10000) { // 10 second timeout
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
                _osmError.value = "timeout"
                android.util.Log.w("MapViewModel", "OSM search timed out for '$trailName' (expected for trails not in OSM)")
            } catch (e: Exception) {
                _isLoadingOsmTrails.value = false
                _osmError.value = "unavailable"
                android.util.Log.w("MapViewModel", "OSM search failed for '$trailName': ${e.message}")
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
     * Clear all OSM trails from map
     */
    fun clearOsmTrails() {
        _osmTrails.value = emptyList()
        _selectedOsmTrail.value = null
        _osmError.value = null
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
    
    /**
     * Calculate combined statistics for all trails
     */
    private fun updateCombinedStats(trails: List<OsmTrail>) {
        val totalDistance = trails.sumOf { it.distance ?: 0.0 }
        // OSM trails don't have elevation data directly, set to 0.0
        // Elevation can be calculated later using Google Elevation API if needed
        val totalElevation = 0.0
        
        _combinedTrailStats.value = CombinedTrailStats(
            totalDistance = totalDistance,
            totalElevation = totalElevation,
            trailCount = trails.size,
            osmTrails = trails
        )
    }
    
    // ===== BOUNDARY-BASED TRAIL LOADING =====
    
    /**
     * State for OSM boundaries
     */
    private val _selectedBoundary = MutableStateFlow<com.trailguide.android.data.osm.OsmBoundary?>(null)
    val selectedBoundary: StateFlow<com.trailguide.android.data.osm.OsmBoundary?> = _selectedBoundary.asStateFlow()
    
    private val _boundaryTrails = MutableStateFlow<List<OsmTrail>>(emptyList())
    val boundaryTrails: StateFlow<List<OsmTrail>> = _boundaryTrails.asStateFlow()
    
    private val _isLoadingBoundary = MutableStateFlow(false)
    val isLoadingBoundary: StateFlow<Boolean> = _isLoadingBoundary.asStateFlow()
    
    private val _boundaryError = MutableStateFlow<String?>(null)
    val boundaryError: StateFlow<String?> = _boundaryError.asStateFlow()
    
    /**
     * Load trails for a Google Place by finding its OSM boundary
     * This is called when user clicks on a Google Places marker
     * 
     * @param placeName Name of the place from Google Places
     * @param latitude Latitude from Google Places
     * @param longitude Longitude from Google Places
     */
    fun loadTrailsForGooglePlace(placeName: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isLoadingBoundary.value = true
            _boundaryError.value = null
            
            android.util.Log.d("MapViewModel", "Loading OSM trails for: $placeName at ($latitude, $longitude)")
            
            try {
                // NEW STRATEGY: Search by name + verify proximity
                // Much more accurate for specific locations
                android.util.Log.d("MapViewModel", "Searching by name: $placeName")
                
                osmTrailRepository.getBoundaryByNameAndLocation(
                    placeName = placeName,
                    latitude = latitude,
                    longitude = longitude
                ).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            val boundary = result.data
                            
                            android.util.Log.d("MapViewModel", 
                                "✓ Found boundary: ${boundary.name} with ${boundary.polygon.size} points")
                            
                            _selectedBoundary.value = boundary
                            loadTrailsInBoundary(boundary)
                        }
                        is NetworkResult.Error -> {
                            android.util.Log.w("MapViewModel", "Boundary search failed: ${result.message}")
                            // Fallback to bounding box search
                            loadTrailsInBoundingBox(latitude, longitude)
                        }
                        is NetworkResult.Loading -> {
                            _isLoadingBoundary.value = true
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MapViewModel", "Error loading trails for Google Place", e)
                _isLoadingBoundary.value = false
                _boundaryError.value = e.message
                
                // Fallback to bounding box search
                loadTrailsInBoundingBox(latitude, longitude)
            }
        }
    }
    
    /**
     * Fallback: Load trails in a bounding box around the location
     * Used when boundary lookup fails
     * Creates a SMALL 1km x 1km box for accurate local search
     */
    private fun loadTrailsInBoundingBox(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            android.util.Log.d("MapViewModel", "Using bounding box fallback for ($latitude, $longitude)")
            
            // Create a SMALL 1km x 1km bounding box (500m radius)
            val radiusKm = 0.5 // 500 meters
            val latOffset = radiusKm / 111.0 // 1 degree latitude ≈ 111km
            val lonOffset = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(latitude)))
            
            val bbox = BoundingBox(
                south = latitude - latOffset,
                north = latitude + latOffset,
                west = longitude - lonOffset,
                east = longitude + lonOffset
            )
            
            android.util.Log.d("MapViewModel", "Created 1km x 1km bounding box")
            
            // Create a fake boundary for UI display
            val boundaryPolygon = listOf(
                LatLng(bbox.south, bbox.west),
                LatLng(bbox.north, bbox.west),
                LatLng(bbox.north, bbox.east),
                LatLng(bbox.south, bbox.east),
                LatLng(bbox.south, bbox.west)
            )
            
            val fakeBoundary = OsmBoundary(
                osmId = 0,
                osmType = "bbox",
                name = "1km area around location",
                centerLat = latitude,
                centerLon = longitude,
                polygon = boundaryPolygon,
                boundingBox = bbox
            )
            
            _selectedBoundary.value = fakeBoundary
            loadTrailsInBoundary(fakeBoundary)
        }
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLon = Math.toRadians(point2.longitude - point1.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
    
    /**
     * Get boundary at a specific location using reverse geocoding
     * Fallback when name search fails
     */
    /**
     * Load trails within a specific boundary
     * Core function for showing trails inside nature reserves/parks
     */
    private fun loadTrailsInBoundary(boundary: com.trailguide.android.data.osm.OsmBoundary) {
        viewModelScope.launch {
            try {
                android.util.Log.d("MapViewModel", "Fetching trails in boundary: ${boundary.name}")
                
                osmTrailRepository.fetchTrailsInBoundary(boundary).collect { result ->
                    _isLoadingBoundary.value = false
                    when (result) {
                        is NetworkResult.Success -> {
                            _boundaryTrails.value = result.data
                            android.util.Log.d("MapViewModel", "Loaded ${result.data.size} trails in ${boundary.name}")
                            
                            // Also update the main OSM trails list so they show on map
                            _osmTrails.value = result.data
                            updateCombinedStats(result.data)
                            _showOsmTrails.value = true
                        }
                        is NetworkResult.Error -> {
                            _boundaryError.value = result.message
                            android.util.Log.e("MapViewModel", "Error loading boundary trails: ${result.message}")
                        }
                        is NetworkResult.Loading -> {
                            _isLoadingBoundary.value = true
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MapViewModel", "Exception loading boundary trails", e)
                _isLoadingBoundary.value = false
                _boundaryError.value = e.message
            }
        }
    }
    
    /**
     * Clear boundary selection
     */
    fun clearBoundary() {
        _selectedBoundary.value = null
        _boundaryTrails.value = emptyList()
        _boundaryError.value = null
    }
    
    /**
     * Download current boundary area for offline use
     */
    suspend fun downloadCurrentArea(): kotlinx.coroutines.flow.Flow<com.trailguide.android.data.repository.DownloadProgress>? {
        val boundary = _selectedBoundary.value
        val trails = _boundaryTrails.value
        val stats = _combinedTrailStats.value
        
        return if (boundary != null && trails.isNotEmpty() && stats != null) {
            offlineMapRepository.saveOfflineArea(
                areaName = boundary.name,
                boundary = boundary,
                trails = trails,
                combinedStats = stats
            )
        } else null
    }
}

