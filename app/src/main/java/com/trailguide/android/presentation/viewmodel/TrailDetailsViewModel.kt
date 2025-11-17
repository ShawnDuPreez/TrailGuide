package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.model.WeatherForecast
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.repository.GoogleTrailRepository
import com.trailguide.android.data.repository.TrailRepository
import com.trailguide.android.data.repository.WeatherRepository
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
    private val googleTrailRepository: GoogleTrailRepository,
    private val weatherRepository: WeatherRepository,
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
    
    private val _isDownloaded = MutableStateFlow(false)
    val isDownloaded: StateFlow<Boolean> = _isDownloaded.asStateFlow()
    
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // Weather data
    private val _weatherForecast = MutableStateFlow<WeatherForecast?>(null)
    val weatherForecast: StateFlow<WeatherForecast?> = _weatherForecast.asStateFlow()
    
    private val _isLoadingWeather = MutableStateFlow(false)
    val isLoadingWeather: StateFlow<Boolean> = _isLoadingWeather.asStateFlow()
    
    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError.asStateFlow()
    
    init {
        if (trailId.isNotEmpty()) {
            loadTrailDetails()
            checkIfDownloaded()
        }
    }
    
    /**
     * Check if trail is already downloaded.
     */
    private fun checkIfDownloaded() {
        viewModelScope.launch {
            _isDownloaded.value = trailRepository.isTrailDownloaded(trailId)
        }
    }
    
    /**
     * Public method to load trail by ID (for external calls).
     */
    fun loadTrail(trailId: String) {
        if (trailId.isNotEmpty()) {
            loadTrailDetails()
        }
    }
    
    /**
     * Load trail details from repository.
     */
    private fun loadTrailDetails() {
        viewModelScope.launch {
            val handledByGoogle = tryLoadGoogleTrail()
            if (!handledByGoogle) {
                loadTrailFromApi()
            }
        }
    }
    
    private suspend fun tryLoadGoogleTrail(): Boolean {
        var handled = false
        googleTrailRepository.getCompleteTrailDetails(trailId).collect { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    _isLoading.value = true
                    _errorMessage.value = null
                }
                is NetworkResult.Success -> {
                    val trail = result.data
                    _trail.value = trail
                    _isFavorite.value = trail.isFavorite
                    _isLoading.value = false
                    _errorMessage.value = null
                    handled = true
                    
                    loadWeather(trail.latitude, trail.longitude)
                }
                is NetworkResult.Error -> {
                    handled = false
                }
            }
        }
        return handled
    }
    
    private suspend fun loadTrailFromApi() {
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
                    
                    // Load weather for trail location
                    loadWeather(result.data.latitude, result.data.longitude)
                }
                is NetworkResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
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
            trailRepository.toggleFavorite(currentTrail, newFavoriteStatus).collect { result ->
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
     * Download trail for offline use.
     */
    fun downloadTrail() {
        val currentTrail = _trail.value ?: return
        
        viewModelScope.launch {
            _isDownloading.value = true
            trailRepository.downloadTrail(currentTrail).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isDownloading.value = true
                    }
                    is NetworkResult.Success -> {
                        _isDownloaded.value = true
                        _isDownloading.value = false
                        _successMessage.value = "${currentTrail.name} downloaded for offline use!"
                    }
                    is NetworkResult.Error -> {
                        _isDownloading.value = false
                        _errorMessage.value = result.message
                    }
                }
            }
        }
    }
    
    /**
     * Delete downloaded trail.
     */
    fun deleteDownload() {
        val currentTrail = _trail.value ?: return
        
        viewModelScope.launch {
            trailRepository.deleteDownloadedTrail(currentTrail.id).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _isDownloaded.value = false
                        _successMessage.value = "Removed from downloads"
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = result.message
                    }
                    is NetworkResult.Loading -> { /* No action */ }
                }
            }
        }
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
    
    /**
     * Clear success message.
     */
    fun clearSuccess() {
        _successMessage.value = null
    }
    
    /**
     * Load weather forecast for trail location.
     */
    private fun loadWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            weatherRepository.getWeatherForecast(latitude, longitude).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoadingWeather.value = true
                        _weatherError.value = null
                    }
                    is NetworkResult.Success -> {
                        _weatherForecast.value = result.data
                        _isLoadingWeather.value = false
                        _weatherError.value = null
                    }
                    is NetworkResult.Error -> {
                        _isLoadingWeather.value = false
                        _weatherError.value = result.message
                    }
                }
            }
        }
    }
    
    /**
     * Refresh weather data.
     */
    fun refreshWeather() {
        val currentTrail = _trail.value ?: return
        loadWeather(currentTrail.latitude, currentTrail.longitude)
    }
}

