package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PolylineOptions
import com.trailguide.android.data.repository.OsmTrailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Map screen.
 * Manages OSM trail data and map state.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val osmTrailRepository: OsmTrailRepository
) : ViewModel() {
    
    // OSM Trail state
    private val _osmTrails = MutableStateFlow<List<PolylineOptions>>(emptyList())
    val osmTrails: StateFlow<List<PolylineOptions>> = _osmTrails.asStateFlow()
    
    private val _isLoadingOsmTrails = MutableStateFlow(false)
    val isLoadingOsmTrails: StateFlow<Boolean> = _isLoadingOsmTrails.asStateFlow()
    
    private val _osmTrailError = MutableStateFlow<String?>(null)
    val osmTrailError: StateFlow<String?> = _osmTrailError.asStateFlow()
    
    private val _showOsmTrails = MutableStateFlow(false)
    val showOsmTrails: StateFlow<Boolean> = _showOsmTrails.asStateFlow()
    
    private val _selectedTrail = MutableStateFlow<PolylineOptions?>(null)
    val selectedTrail: StateFlow<PolylineOptions?> = _selectedTrail.asStateFlow()
    
    private val _availableTrails = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val availableTrails: StateFlow<List<Pair<String, String>>> = _availableTrails.asStateFlow()
    
    /**
     * Load OSM hiking trails from Overpass API.
     */
    fun loadOsmTrails() {
        if (_isLoadingOsmTrails.value) return
        
        viewModelScope.launch {
            _isLoadingOsmTrails.value = true
            _osmTrailError.value = null
            
            try {
                // Add a timeout mechanism
                val timeoutJob = launch {
                    kotlinx.coroutines.delay(30000) // 30 second timeout
                    if (_isLoadingOsmTrails.value) {
                        _isLoadingOsmTrails.value = false
                        _osmTrailError.value = "Request timed out. The query might be too large. Try again later."
                    }
                }
                
                osmTrailRepository.fetchHikingTrailsForMap().collect { result ->
                    timeoutJob.cancel()
                    _isLoadingOsmTrails.value = false
                    if (result.isSuccess) {
                        val trails = result.getOrThrow()
                        _osmTrails.value = trails
                        _showOsmTrails.value = true
                        android.util.Log.d("MapViewModel", "Successfully loaded ${trails.size} OSM trails")
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Failed to load OSM trails"
                        _osmTrailError.value = error
                        android.util.Log.e("MapViewModel", "Failed to load OSM trails: $error")
                    }
                }
            } catch (e: Exception) {
                _isLoadingOsmTrails.value = false
                _osmTrailError.value = e.message ?: "Unknown error"
                android.util.Log.e("MapViewModel", "Exception loading OSM trails", e)
            }
        }
    }
    
    /**
     * Toggle OSM trails visibility.
     */
    fun toggleOsmTrails() {
        if (_showOsmTrails.value) {
            _showOsmTrails.value = false
        } else {
            if (_osmTrails.value.isEmpty()) {
                loadOsmTrails()
            } else {
                _showOsmTrails.value = true
            }
        }
    }
    
    /**
     * Clear OSM trail error.
     */
    fun clearOsmTrailError() {
        _osmTrailError.value = null
    }
    
    /**
     * Load available trails for selection.
     */
    fun loadAvailableTrails() {
        viewModelScope.launch {
            osmTrailRepository.getAvailableTrails().collect { result ->
                if (result.isSuccess) {
                    _availableTrails.value = result.getOrThrow()
                }
            }
        }
    }
    
    /**
     * Select a specific trail for hiking.
     * This replaces the big red outline with individual trail paths.
     */
    fun selectTrail(trailId: String) {
        viewModelScope.launch {
            osmTrailRepository.getTrailById(trailId).collect { result ->
                if (result.isSuccess) {
                    _selectedTrail.value = result.getOrThrow()
                    _showOsmTrails.value = false // Hide all trails, show only selected
                    android.util.Log.d("MapViewModel", "Selected trail: $trailId")
                }
            }
        }
    }
    
    /**
     * Clear selected trail and return to showing all trails.
     */
    fun clearSelectedTrail() {
        _selectedTrail.value = null
        _showOsmTrails.value = true
    }
}
