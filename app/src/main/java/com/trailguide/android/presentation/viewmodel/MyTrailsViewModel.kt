package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.local.FavoriteTrailEntity
import com.trailguide.android.data.local.TrailProgressEntity
import com.trailguide.android.data.repository.AuthRepository
import com.trailguide.android.data.repository.FavoritesRepository
import com.trailguide.android.data.repository.TrailProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for My Trails screen.
 * Manages user's trail progress, favorites, and statistics.
 */
@HiltViewModel
class MyTrailsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val favoritesRepository: FavoritesRepository,
    private val trailProgressRepository: TrailProgressRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _inProgressTrails = MutableStateFlow<List<TrailProgressEntity>>(emptyList())
    val inProgressTrails: StateFlow<List<TrailProgressEntity>> = _inProgressTrails.asStateFlow()
    
    private val _completedTrails = MutableStateFlow<List<TrailProgressEntity>>(emptyList())
    val completedTrails: StateFlow<List<TrailProgressEntity>> = _completedTrails.asStateFlow()
    
    private val _favoriteTrails = MutableStateFlow<List<FavoriteTrailEntity>>(emptyList())
    val favoriteTrails: StateFlow<List<FavoriteTrailEntity>> = _favoriteTrails.asStateFlow()
    
    private val _totalDistance = MutableStateFlow(0f)
    val totalDistance: StateFlow<Float> = _totalDistance.asStateFlow()
    
    private val _totalTrailsCompleted = MutableStateFlow(0)
    val totalTrailsCompleted: StateFlow<Int> = _totalTrailsCompleted.asStateFlow()
    
    init {
        loadMyTrails()
    }
    
    /**
     * Load all user's trail data.
     */
    private fun loadMyTrails() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Get current user ID
                val currentUser = authRepository.currentUser
                val userId = currentUser?.id ?: run {
                    _isLoading.value = false
                    return@launch
                }
                
                // Load trail progress
                trailProgressRepository.getProgressForUser(userId).collect { progressList ->
                    // Separate in-progress and completed trails
                    val inProgress = progressList.filter { !it.isCompleted }
                        .sortedByDescending { it.lastUpdatedAt }
                    val completed = progressList.filter { it.isCompleted }
                        .sortedByDescending { it.completedAt ?: 0L }
                    
                    _inProgressTrails.value = inProgress
                    _completedTrails.value = completed
                    
                    // Calculate total distance (only from completed trails)
                    _totalDistance.value = completed.sumOf { it.distanceCovered.toDouble() }.toFloat()
                    
                    // Count completed trails
                    _totalTrailsCompleted.value = completed.size
                    
                    _isLoading.value = false
                }
                
                // Load favorites
                favoritesRepository.getFavorites(userId).collect { favorites ->
                    _favoriteTrails.value = favorites.sortedByDescending { it.favoritedAt }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh all data.
     */
    fun refresh() {
        loadMyTrails()
    }
    
    /**
     * Remove a trail from favorites.
     */
    fun removeFavorite(trailId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUser?.id ?: return@launch
                favoritesRepository.removeFavorite(userId, trailId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

