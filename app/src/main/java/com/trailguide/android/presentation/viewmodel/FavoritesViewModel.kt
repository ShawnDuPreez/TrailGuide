package com.trailguide.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailguide.android.data.local.CollectionDao
import com.trailguide.android.data.local.toDomainModel
import com.trailguide.android.data.local.toEntity
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.model.TrailCollection
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.repository.TrailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Favorites screen
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val trailRepository: TrailRepository,
    private val collectionDao: CollectionDao
) : ViewModel() {
    
    private val _favoriteTrails = MutableStateFlow<List<Trail>>(emptyList())
    val favoriteTrails: StateFlow<List<Trail>> = _favoriteTrails.asStateFlow()
    
    private val _collections = MutableStateFlow<List<TrailCollection>>(emptyList())
    val collections: StateFlow<List<TrailCollection>> = _collections.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadFavorites()
        loadCollections()
    }
    
    private fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            trailRepository.getFavoriteTrails().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _favoriteTrails.value = result.data
                        _isLoading.value = false
                        _errorMessage.value = null
                    }
                    is NetworkResult.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = result.message
                    }
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }
    
    private fun loadCollections() {
        viewModelScope.launch {
            collectionDao.getAllCollections().collect { entities ->
                _collections.value = entities.map { it.toDomainModel() }
            }
        }
    }
    
    fun toggleFavorite(trailId: String) {
        viewModelScope.launch {
            val isFavorite = _favoriteTrails.value.any { it.id == trailId }
            trailRepository.toggleFavorite(trailId, !isFavorite).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        loadFavorites()
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = "Failed to update favorite: ${result.message}"
                    }
                    is NetworkResult.Loading -> {}
                }
            }
        }
    }
    
    fun createCollection(name: String, description: String) {
        viewModelScope.launch {
            val collection = TrailCollection(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description.takeIf { it.isNotBlank() }
            )
            collectionDao.insertCollection(collection.toEntity())
        }
    }
    
    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            collectionDao.deleteCollectionById(collectionId)
        }
    }
    
    fun addTrailToCollection(collectionId: String, trailId: String) {
        viewModelScope.launch {
            collectionDao.getCollectionById(collectionId).first()?.let { entity ->
                val collection = entity.toDomainModel()
                val updatedCollection = collection.copy(
                    trailIds = (collection.trailIds + trailId).distinct()
                )
                collectionDao.updateCollection(updatedCollection.toEntity())
            }
        }
    }
    
    fun removeTrailFromCollection(collectionId: String, trailId: String) {
        viewModelScope.launch {
            collectionDao.getCollectionById(collectionId).first()?.let { entity ->
                val collection = entity.toDomainModel()
                val updatedCollection = collection.copy(
                    trailIds = collection.trailIds.filter { it != trailId }
                )
                collectionDao.updateCollection(updatedCollection.toEntity())
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}

