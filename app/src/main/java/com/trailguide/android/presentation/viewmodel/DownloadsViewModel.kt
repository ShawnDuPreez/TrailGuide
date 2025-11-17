package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.repository.OfflineMapRepository
import com.trailguide.android.data.repository.OfflineAreaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Downloads screen.
 * Manages offline area downloads and local storage.
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val offlineRepository: OfflineMapRepository
) : ViewModel() {
    
    private val _offlineAreas = MutableStateFlow<List<OfflineAreaMetadata>>(emptyList())
    val offlineAreas: StateFlow<List<OfflineAreaMetadata>> = _offlineAreas.asStateFlow()
    
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
        loadOfflineAreas()
    }
    
    /**
     * Load all offline areas from local storage.
     */
    private fun loadOfflineAreas() {
        viewModelScope.launch {
            try {
                val areas = offlineRepository.getOfflineAreas()
                _offlineAreas.value = areas
                _storageUsedBytes.value = areas.sumOf { it.fileSize }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load offline areas: ${e.message}"
            }
        }
    }
    
    /**
     * Delete an offline area.
     */
    fun deleteOfflineArea(areaName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val success = offlineRepository.deleteOfflineArea(areaName)
                _isLoading.value = false
                if (success) {
                    _successMessage.value = "$areaName removed from offline areas"
                    loadOfflineAreas()
                } else {
                    _errorMessage.value = "Failed to delete $areaName"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Error deleting area: ${e.message}"
            }
        }
    }
    
    /**
     * Delete all offline areas.
     */
    fun deleteAllOfflineAreas() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _offlineAreas.value.forEach { area ->
                    offlineRepository.deleteOfflineArea(area.name)
                }
                _isLoading.value = false
                _successMessage.value = "All offline areas cleared"
                loadOfflineAreas()
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Error clearing all areas: ${e.message}"
            }
        }
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

