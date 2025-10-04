package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.repository.TrailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Trail Details screen.
 * Manages detailed trail information and user interactions.
 */
@HiltViewModel
class TrailDetailsViewModel @Inject constructor(
    private val trailRepository: TrailRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Trail ID from navigation arguments
    private val trailId: String = savedStateHandle.get<String>("trailId") ?: ""
    
    // Trail data and UI state
    private val _trail = MutableStateFlow<Trail?>(null)
    val trail: StateFlow<Trail?> = _trail.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()
    
    init {
        if (trailId.isNotEmpty()) {
            loadTrailDetails()
        }
    }
    
    /**
     * Load trail details from repository.
     */
    private fun loadTrailDetails() {
        viewModelScope.launch {
            trailRepository.getTrailById(trailId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _trail.value = result.data
                        _isFavorite.value = result.data.isFavorite
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
     * Toggle favorite status.
     */
    fun toggleFavorite() {
        val currentTrail = _trail.value ?: return
        val newFavoriteStatus = !_isFavorite.value
        
        viewModelScope.launch {
            trailRepository.toggleFavorite(currentTrail.id, newFavoriteStatus).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _isFavorite.value = newFavoriteStatus
                        _trail.value = currentTrail.copy(isFavorite = newFavoriteStatus)
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
     * Mark trail as downloaded for offline use.
     */
    fun markAsDownloaded() {
        val currentTrail = _trail.value ?: return
        _trail.value = currentTrail.copy(isDownloaded = true)
        // In a real app, this would trigger actual map/data download
    }
    
    /**
     * Start navigation/hiking for this trail.
     */
    fun startHike() {
        // This would integrate with GPS tracking in a real implementation
        val currentTrail = _trail.value ?: return
        // Log or emit event for starting hike
    }
    
    /**
     * Reload trail details.
     */
    fun refresh() {
        loadTrailDetails()
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

