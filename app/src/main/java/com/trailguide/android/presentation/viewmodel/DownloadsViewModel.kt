package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.repository.TrailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Downloads screen.
 * Manages offline trail downloads and local storage.
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: TrailRepository
) : ViewModel() {
    
    private val _downloadedTrails = MutableStateFlow<List<Trail>>(emptyList())
    val downloadedTrails: StateFlow<List<Trail>> = _downloadedTrails.asStateFlow()
    
    private val _storageUsedBytes = MutableStateFlow(0L)
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes.asStateFlow()
    
    private val _downloadCount = MutableStateFlow(0)
    val downloadCount: StateFlow<Int> = _downloadCount.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        loadDownloadedTrails()
        loadStorageInfo()
    }
    
    /**
     * Load all downloaded trails from local storage.
     */
    private fun loadDownloadedTrails() {
        viewModelScope.launch {
            repository.getDownloadedTrails().collect { trails ->
                _downloadedTrails.value = trails
            }
        }
    }
    
    /**
     * Load storage information (used space and download count).
     */
    fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                _storageUsedBytes.value = repository.getTotalStorageUsed()
                _downloadCount.value = repository.getDownloadCount()
            } catch (e: Exception) {
                // Silently fail - not critical
            }
        }
    }
    
    /**
     * Download a trail for offline use.
     */
    fun downloadTrail(trail: Trail) {
        viewModelScope.launch {
            repository.downloadTrail(trail).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoading.value = false
                        _successMessage.value = "${trail.name} downloaded for offline use"
                        loadStorageInfo()
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
     * Delete a downloaded trail.
     */
    fun deleteDownload(trailId: String, trailName: String) {
        viewModelScope.launch {
            repository.deleteDownloadedTrail(trailId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoading.value = false
                        _successMessage.value = "$trailName removed from downloads"
                        loadStorageInfo()
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
     * Delete all downloaded trails.
     */
    fun deleteAllDownloads() {
        viewModelScope.launch {
            repository.deleteAllDownloads().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoading.value = false
                        _successMessage.value = "All downloads cleared"
                        loadStorageInfo()
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
     * Check if a trail is downloaded.
     */
    suspend fun isTrailDownloaded(trailId: String): Boolean {
        return repository.isTrailDownloaded(trailId)
    }
    
    /**
     * Format storage size for display.
     */
    fun formatStorageSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
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
}

