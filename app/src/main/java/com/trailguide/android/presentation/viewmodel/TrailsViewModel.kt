package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.repository.TrailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Trails List screen.
 * Manages trail data, search filters, and UI state using StateFlow.
 */
@HiltViewModel
class TrailsViewModel @Inject constructor(
    private val trailRepository: TrailRepository
) : ViewModel() {
    
    // Search and filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedDifficulty = MutableStateFlow<Difficulty?>(null)
    val selectedDifficulty: StateFlow<Difficulty?> = _selectedDifficulty.asStateFlow()
    
    private val _maxDistance = MutableStateFlow(20.0)
    val maxDistance: StateFlow<Double> = _maxDistance.asStateFlow()
    
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
        _maxDistance
    ) { trails, query, difficulty, distance ->
        trails.filter { trail ->
            val matchesQuery = query.isBlank() || 
                trail.name.contains(query, ignoreCase = true) ||
                trail.city.contains(query, ignoreCase = true)
            
            val matchesDifficulty = difficulty == null || trail.difficulty == difficulty
            
            val matchesDistance = trail.distanceKm <= distance
            
            matchesQuery && matchesDifficulty && matchesDistance
        }.sortedBy { it.difficulty.order }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        loadTrails()
    }
    
    /**
     * Load all trails from the repository.
     */
    fun loadTrails() {
        viewModelScope.launch {
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
    }
    
    /**
     * Update search query.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Update selected difficulty filter.
     */
    fun setDifficulty(difficulty: Difficulty?) {
        _selectedDifficulty.value = difficulty
    }
    
    /**
     * Update maximum distance filter.
     */
    fun setMaxDistance(distance: Double) {
        _maxDistance.value = distance
    }
    
    /**
     * Clear all filters.
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedDifficulty.value = null
        _maxDistance.value = 20.0
    }
    
    /**
     * Toggle favorite status for a trail.
     */
    fun toggleFavorite(trailId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            trailRepository.toggleFavorite(trailId, isFavorite).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        // Update local trail list
                        _trails.value = _trails.value.map { trail ->
                            if (trail.id == trailId) {
                                trail.copy(isFavorite = isFavorite)
                            } else {
                                trail
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

