package com.trailguide.android.data.repository

import android.util.Log
import com.trailguide.android.data.dto.CreateTrailRequest
import com.trailguide.android.data.dto.toDomainModel
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.remote.TrailApiService
import com.trailguide.android.data.remote.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for trail data operations.
 * Implements the Repository pattern to provide a clean API for data access.
 * Handles network calls, data transformation, and error handling.
 */
@Singleton
class TrailRepository @Inject constructor(
    private val apiService: TrailApiService
) {
    
    companion object {
        private const val TAG = "TrailRepository"
    }
    
    /**
     * Wake up the server with a health check (for Render.com cold starts).
     * This should be called when the app starts to pre-warm the server.
     * Runs silently in the background - doesn't emit any results to UI.
     */
    suspend fun wakeUpServer() {
        try {
            Log.d(TAG, "Sending wake-up ping to server...")
            apiService.healthCheck()
            Log.d(TAG, "Server wake-up ping sent successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Server wake-up ping failed (this is normal): ${e.message}")
            // Silently fail - the actual data request will handle the cold start
        }
    }
    
    /**
     * Fetch all trails from the API.
     * Returns a Flow for reactive data handling.
     */
    fun getAllTrails(): Flow<NetworkResult<List<Trail>>> = flow {
        emit(NetworkResult.Loading)
        
        val result = safeApiCall { apiService.getAllTrails() }
        
        when (result) {
            is NetworkResult.Success -> {
                val trails = result.data.map { it.toDomainModel() }
                emit(NetworkResult.Success(trails))
                Log.d(TAG, "Successfully fetched ${trails.size} trails")
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message, result.exception))
                Log.e(TAG, "Error fetching trails: ${result.message}")
            }
            is NetworkResult.Loading -> emit(NetworkResult.Loading)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get a specific trail by ID.
     */
    fun getTrailById(trailId: String): Flow<NetworkResult<Trail>> = flow {
        emit(NetworkResult.Loading)
        
        val result = safeApiCall { apiService.getTrailById(trailId) }
        
        when (result) {
            is NetworkResult.Success -> {
                val trail = result.data.toDomainModel()
                emit(NetworkResult.Success(trail))
                Log.d(TAG, "Successfully fetched trail: ${trail.name}")
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message, result.exception))
                Log.e(TAG, "Error fetching trail $trailId: ${result.message}")
            }
            is NetworkResult.Loading -> emit(NetworkResult.Loading)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search trails with filters.
     */
    fun searchTrails(
        query: String,
        difficulty: Difficulty? = null,
        maxDistance: Double? = null
    ): Flow<NetworkResult<List<Trail>>> = flow {
        emit(NetworkResult.Loading)
        
        val result = safeApiCall {
            apiService.searchTrails(
                query = query,
                difficulty = difficulty?.name?.lowercase(),
                maxDistance = maxDistance
            )
        }
        
        when (result) {
            is NetworkResult.Success -> {
                val trails = result.data.map { it.toDomainModel() }
                emit(NetworkResult.Success(trails))
                Log.d(TAG, "Search returned ${trails.size} trails")
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message, result.exception))
                Log.e(TAG, "Search error: ${result.message}")
            }
            is NetworkResult.Loading -> emit(NetworkResult.Loading)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create a new trail.
     */
    fun createTrail(trail: Trail): Flow<NetworkResult<Trail>> = flow {
        emit(NetworkResult.Loading)
        
        val request = CreateTrailRequest(
            name = trail.name,
            city = trail.city,
            latitude = trail.latitude,
            longitude = trail.longitude,
            distanceKm = trail.distanceKm,
            elevationM = trail.elevationM,
            difficulty = trail.difficulty.name.lowercase(),
            rating = trail.rating,
            imageUrl = trail.imageUrl,
            tags = trail.tags,
            description = trail.description
        )
        
        val result = safeApiCall { apiService.createTrail(request) }
        
        when (result) {
            is NetworkResult.Success -> {
                val newTrail = result.data.toDomainModel()
                emit(NetworkResult.Success(newTrail))
                Log.d(TAG, "Successfully created trail: ${newTrail.name}")
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message, result.exception))
                Log.e(TAG, "Error creating trail: ${result.message}")
            }
            is NetworkResult.Loading -> emit(NetworkResult.Loading)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update an existing trail.
     */
    fun updateTrail(trail: Trail): Flow<NetworkResult<Trail>> = flow {
        emit(NetworkResult.Loading)
        
        val request = CreateTrailRequest(
            name = trail.name,
            city = trail.city,
            latitude = trail.latitude,
            longitude = trail.longitude,
            distanceKm = trail.distanceKm,
            elevationM = trail.elevationM,
            difficulty = trail.difficulty.name.lowercase(),
            rating = trail.rating,
            imageUrl = trail.imageUrl,
            tags = trail.tags,
            description = trail.description
        )
        
        val result = safeApiCall { apiService.updateTrail(trail.id, request) }
        
        when (result) {
            is NetworkResult.Success -> {
                val updatedTrail = result.data.toDomainModel()
                emit(NetworkResult.Success(updatedTrail))
                Log.d(TAG, "Successfully updated trail: ${updatedTrail.name}")
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message, result.exception))
                Log.e(TAG, "Error updating trail: ${result.message}")
            }
            is NetworkResult.Loading -> emit(NetworkResult.Loading)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Delete a trail by ID.
     */
    fun deleteTrail(trailId: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        val result = safeApiCall { apiService.deleteTrail(trailId) }
        
        when (result) {
            is NetworkResult.Success -> {
                emit(NetworkResult.Success(Unit))
                Log.d(TAG, "Successfully deleted trail $trailId")
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message, result.exception))
                Log.e(TAG, "Error deleting trail: ${result.message}")
            }
            is NetworkResult.Loading -> emit(NetworkResult.Loading)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Toggle favorite status for a trail.
     */
    fun toggleFavorite(trailId: String, isFavorite: Boolean): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        val result = safeApiCall {
            apiService.toggleFavorite(trailId, mapOf("favorite" to isFavorite))
        }
        
        when (result) {
            is NetworkResult.Success -> {
                emit(NetworkResult.Success(Unit))
                Log.d(TAG, "Toggled favorite for trail $trailId: $isFavorite")
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message, result.exception))
                Log.e(TAG, "Error toggling favorite: ${result.message}")
            }
            is NetworkResult.Loading -> emit(NetworkResult.Loading)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get user's favorite trails.
     */
    fun getFavoriteTrails(): Flow<NetworkResult<List<Trail>>> = flow {
        emit(NetworkResult.Loading)
        
        val result = safeApiCall { apiService.getFavoriteTrails() }
        
        when (result) {
            is NetworkResult.Success -> {
                val trails = result.data.map { it.toDomainModel().copy(isFavorite = true) }
                emit(NetworkResult.Success(trails))
                Log.d(TAG, "Successfully fetched ${trails.size} favorite trails")
            }
            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.message, result.exception))
                Log.e(TAG, "Error fetching favorites: ${result.message}")
            }
            is NetworkResult.Loading -> emit(NetworkResult.Loading)
        }
    }.flowOn(Dispatchers.IO)
}

