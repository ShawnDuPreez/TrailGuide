package com.trailguide.android.data.repository

import android.util.Log
import com.trailguide.android.data.dto.CreateTrailRequest
import com.trailguide.android.data.dto.toDomainModel
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.data.model.RoutePoint
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.local.toDomainModel
import com.trailguide.android.data.local.toEntity
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.remote.OpenRouteClient
import com.trailguide.android.data.remote.TrailApiService
import com.trailguide.android.data.remote.safeApiCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Repository for trail data operations.
 * Implements the Repository pattern to provide a clean API for data access.
 * Handles network calls, data transformation, error handling, and offline storage.
 */
@Singleton
class TrailRepository @Inject constructor(
    private val apiService: TrailApiService,
    private val downloadedTrailDao: com.trailguide.android.data.local.DownloadedTrailDao,
    private val favoriteTrailDao: com.trailguide.android.data.local.FavoriteTrailDao,
    private val supabaseClient: SupabaseClient,
    private val supabaseAuthProvider: SupabaseAuthProvider
) {
    
    companion object {
        private const val TAG = "TrailRepository"
    }
    
    /**
     * Get current user ID from Supabase auth.
     */
    private fun getCurrentUserId(): String? {
        return supabaseAuthProvider.currentUserId()
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
     * Generate a circular hiking route around a center point.
     * Creates a realistic-looking trail loop with elevation changes.
     * This is a fallback for when OpenRouteService API is unavailable.
     * 
     * @param centerLat Center latitude
     * @param centerLon Center longitude
     * @param radiusKm Approximate radius of the loop in kilometers
     * @param numPoints Number of points to generate for the route
     */
    private fun generateCircularRoute(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        numPoints: Int = 50
    ): List<RoutePoint> {
        val points = mutableListOf<RoutePoint>()
        val radiusDegrees = radiusKm / 111.0 // Rough conversion: 1 degree ≈ 111 km
        
        var baseElevation = Random.nextInt(50, 200)
        
        for (i in 0 until numPoints) {
            val angle = (2 * Math.PI * i) / numPoints
            
            // Add some randomness to make it look more natural
            val radiusVariation = radiusDegrees * (0.8 + Random.nextDouble() * 0.4)
            
            val lat = centerLat + radiusVariation * sin(angle)
            val lon = centerLon + radiusVariation * cos(angle)
            
            // Simulate elevation changes
            val elevationChange = Random.nextInt(-20, 30)
            baseElevation = (baseElevation + elevationChange).coerceIn(0, 1000)
            
            points.add(RoutePoint(lat, lon, baseElevation.toDouble()))
        }
        
        // Close the loop
        if (points.isNotEmpty()) {
            points.add(points.first())
        }
        
        return points
    }
    
    /**
     * Fetch route from OpenRouteService API.
     * Returns a list of GPS coordinates forming the trail route.
     */
    suspend fun fetchRouteForTrail(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): List<RoutePoint> {
        return try {
            Log.d(TAG, "Fetching route from OpenRouteService: ($startLat,$startLon) -> ($endLat,$endLon)")
            
            val startCoord = "$startLon,$startLat" // OpenRouteService uses lon,lat format
            val endCoord = "$endLon,$endLat"
            
            val response = OpenRouteClient.api.getRouteGeoJson(
                start = startCoord,
                end = endCoord
            )
            
            if (response.isSuccessful && response.body() != null) {
                val coordinates = response.body()!!.features.firstOrNull()?.geometry?.coordinates
                
                if (coordinates != null) {
                    Log.d(TAG, "Successfully fetched route with ${coordinates.size} points")
                    coordinates.map { coord ->
                        RoutePoint(
                            latitude = coord[1],  // lat is second in [lon, lat]
                            longitude = coord[0], // lon is first
                            elevation = coord.getOrNull(2) // elevation if available
                        )
                    }
                } else {
                    Log.w(TAG, "No route coordinates in response, generating fallback route")
                    generateFallbackRoute(startLat, startLon, endLat, endLon)
                }
            } else {
                Log.w(TAG, "OpenRouteService request failed: ${response.code()}, generating fallback route")
                generateFallbackRoute(startLat, startLon, endLat, endLon)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching route from OpenRouteService: ${e.message}, generating fallback route")
            generateFallbackRoute(startLat, startLon, endLat, endLon)
        }
    }
    
    /**
     * Generate a simple fallback route between two points.
     * Creates a realistic hiking path with some curves and elevation.
     */
    private fun generateFallbackRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): List<RoutePoint> {
        val points = mutableListOf<RoutePoint>()
        val numPoints = 30
        
        // Calculate center point
        val centerLat = (startLat + endLat) / 2
        val centerLon = (startLon + endLon) / 2
        
        // Calculate distance
        val distance = sqrt(
            (endLat - startLat) * (endLat - startLat) + 
            (endLon - startLon) * (endLon - startLon)
        )
        
        var baseElevation = 100.0
        
        for (i in 0..numPoints) {
            val progress = i.toDouble() / numPoints
            
            // Add some curves to make it look more natural
            val curveFactor = sin(progress * Math.PI) * 0.1
            
            val lat = startLat + (endLat - startLat) * progress + 
                     (endLon - startLon) * curveFactor
            val lon = startLon + (endLon - startLon) * progress - 
                     (endLat - startLat) * curveFactor
            
            // Simulate elevation changes
            baseElevation += Random.nextDouble(-15.0, 20.0)
            baseElevation = baseElevation.coerceIn(0.0, 500.0)
            
            points.add(RoutePoint(lat, lon, baseElevation))
        }
        
        Log.d(TAG, "Generated fallback route with ${points.size} points")
        return points
    }
    
    /**
     * Fetch all trails from the API with their favorite status.
     * Returns a Flow for reactive data handling.
     * Automatically generates routes for trails that don't have them.
     */
    fun getAllTrails(): Flow<NetworkResult<List<Trail>>> = flow {
        emit(NetworkResult.Loading)
        
        val result = safeApiCall { apiService.getAllTrails() }
        
        when (result) {
            is NetworkResult.Success -> {
                val trails = result.data.map { it.toDomainModel() }
                
                // Load favorite status for each trail
                val userId = getCurrentUserId()
                val trailsWithFavorites = if (userId != null) {
                    try {
                        val favoritesResult = safeApiCall { apiService.getFavoriteTrails(userId) }
                        val favoriteTrailIds = when (favoritesResult) {
                            is NetworkResult.Success -> favoritesResult.data.map { it.id }.toSet()
                            else -> emptySet()
                        }
                        
                        trails.map { trail ->
                            trail.copy(isFavorite = trail.id in favoriteTrailIds)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load favorite status: ${e.message}")
                        trails // Return trails without favorite status if loading fails
                    }
                } else {
                    trails // Return trails without favorite status if user is not authenticated
                }
                
                // Generate routes for trails that don't have them
                val trailsWithRoutes = trailsWithFavorites.map { trail ->
                    if (trail.routeCoordinates.isEmpty()) {
                        Log.d(TAG, "Generating route for trail: ${trail.name}")
                        
                        // Generate a circular loop route around the trail's center point
                        val route = generateCircularRoute(
                            centerLat = trail.latitude,
                            centerLon = trail.longitude,
                            radiusKm = (trail.distanceKm ?: 5.0) / 2.0, // Loop radius based on trail distance
                            numPoints = 40
                        )
                        
                        trail.copy(routeCoordinates = route)
                    } else {
                        trail
                    }
                }
                
                emit(NetworkResult.Success(trailsWithRoutes))
                Log.d(TAG, "Successfully fetched ${trailsWithRoutes.size} trails with routes")
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
        
        val request = trail.toCreateRequest()
        
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
        
        val request = trail.toCreateRequest()
        
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
     * POSTs directly to Supabase favourites table and saves to local Room DB.
     * GET requests still go through Render API.
     */
    fun toggleFavorite(trail: Trail, isFavorite: Boolean): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        val userId = getCurrentUserId()
        if (userId == null) {
            emit(NetworkResult.Error("User not authenticated"))
            return@flow
        }
        
        try {
            if (isFavorite) {
                // First, ensure trail exists in Supabase trails table
                val synced = syncTrailToSupabase(trail)
                if (!synced) {
                    emit(NetworkResult.Error("Failed to sync trail to Supabase"))
                    return@flow
                }
                
                // POST directly to Supabase favourites table
                val favoritePayload = mapOf(
                    "user_id" to userId,
                    "trail_id" to trail.id
                )
                
                try {
                    supabaseClient.postgrest["favourites"].insert(favoritePayload) {
                        select()
                    }
                    Log.d(TAG, "Favorite added to Supabase: trail ${trail.id} for user $userId")
                } catch (e: Exception) {
                    // Check if it's a unique constraint violation (already favorited)
                    if (e.message?.contains("23505") == true || e.message?.contains("duplicate") == true) {
                        Log.d(TAG, "Trail already favorited in Supabase")
                    } else {
                        throw e
                    }
                }
                
                // Save to local Room DB for offline access
                val favoriteEntity = com.trailguide.android.data.local.FavoriteTrailEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    trailId = trail.id,
                    trailName = trail.name,
                    trailImageUrl = trail.imageUrl,
                    location = trail.city,
                    addedAt = System.currentTimeMillis(),
                    syncStatus = com.trailguide.android.data.local.SyncStatus.SYNCED.name,
                    lastSyncedAt = System.currentTimeMillis()
                )
                favoriteTrailDao.insertFavorite(favoriteEntity)
                Log.d(TAG, "Favorite saved to local DB")
                
            } else {
                // Remove from Supabase favourites table
                try {
                    supabaseClient.postgrest["favourites"].delete {
                        filter {
                            eq("user_id", userId)
                            eq("trail_id", trail.id)
                        }
                    }
                    Log.d(TAG, "Favorite removed from Supabase: trail ${trail.id} for user $userId")
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing favorite from Supabase: ${e.message}")
                    // Continue to remove from local DB even if Supabase fails
                }
                
                // Remove from local Room DB
                favoriteTrailDao.deleteFavoriteByUserAndTrail(userId, trail.id)
                Log.d(TAG, "Favorite removed from local DB")
            }
            
            emit(NetworkResult.Success(Unit))
            Log.d(TAG, "Successfully toggled favorite for trail ${trail.id}: $isFavorite")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling favorite: ${e.message}", e)
            emit(NetworkResult.Error("Failed to toggle favorite: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    private suspend fun syncTrailToSupabase(trail: Trail): Boolean {
        // Skip Node API, go directly to Supabase
        Log.d(TAG, "Syncing trail ${trail.id} directly to Supabase...")
        return upsertTrailDirectly(trail)
    }

    private suspend fun upsertTrailDirectly(trail: Trail): Boolean {
        return try {
            val payload = mapOf(
                "id" to trail.id,
                "name" to trail.name,
                "city" to (trail.city ?: "Unknown"),
                "lat" to trail.latitude,
                "lon" to trail.longitude,
                "distance_km" to (trail.distanceKm ?: 0.0),
                "elevation_m" to (trail.elevationM ?: 0),
                "difficulty" to (trail.difficulty?.name?.lowercase() ?: "moderate"),
                "rating" to (trail.rating ?: 0.0),
                "image" to trail.imageUrl,
                "tags" to trail.tags,
                "description" to trail.description,
                "updated_at" to java.time.Instant.now().toString()
            )
            supabaseClient.postgrest["trails"].upsert(payload) {
                select()
            }
            Log.d(TAG, "Trail ${trail.id} upserted directly to Supabase")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Direct Supabase upsert failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get user's favorite trails.
     */
    fun getFavoriteTrails(): Flow<NetworkResult<List<Trail>>> = flow {
        emit(NetworkResult.Loading)
        
        val userId = getCurrentUserId()
        if (userId == null) {
            emit(NetworkResult.Error("User not authenticated"))
            return@flow
        }
        
        val result = safeApiCall { apiService.getFavoriteTrails(userId) }
        
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
    
    // ====================
    // OFFLINE/DOWNLOAD OPERATIONS
    // ====================
    
    /**
     * Get all downloaded trails from local storage.
     * Returns a Flow for reactive updates.
     */
    fun getDownloadedTrails(): Flow<List<Trail>> {
        return downloadedTrailDao.getAllDownloadedTrails().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Check if a trail is downloaded.
     */
    suspend fun isTrailDownloaded(trailId: String): Boolean {
        return downloadedTrailDao.isTrailDownloaded(trailId)
    }
    
    /**
     * Download a trail for offline use.
     * Saves trail data (including route) to local database.
     */
    suspend fun downloadTrail(trail: Trail): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Downloading trail: ${trail.name}")
            
            // Save trail to local database
            downloadedTrailDao.insertTrail(trail.toEntity())
            
            emit(NetworkResult.Success(Unit))
            Log.d(TAG, "Successfully downloaded trail: ${trail.name}")
        } catch (e: Exception) {
            emit(NetworkResult.Error("Failed to download trail: ${e.message}", e))
            Log.e(TAG, "Error downloading trail", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Delete a downloaded trail.
     */
    suspend fun deleteDownloadedTrail(trailId: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Deleting downloaded trail: $trailId")
            
            downloadedTrailDao.deleteTrailById(trailId)
            
            emit(NetworkResult.Success(Unit))
            Log.d(TAG, "Successfully deleted trail: $trailId")
        } catch (e: Exception) {
            emit(NetworkResult.Error("Failed to delete trail: ${e.message}", e))
            Log.e(TAG, "Error deleting trail", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Delete all downloaded trails.
     */
    suspend fun deleteAllDownloads(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Deleting all downloads")
            
            downloadedTrailDao.deleteAllTrails()
            
            emit(NetworkResult.Success(Unit))
            Log.d(TAG, "Successfully deleted all downloads")
        } catch (e: Exception) {
            emit(NetworkResult.Error("Failed to delete downloads: ${e.message}", e))
            Log.e(TAG, "Error deleting all downloads", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get total storage used by downloads (in bytes).
     */
    suspend fun getTotalStorageUsed(): Long {
        return downloadedTrailDao.getTotalStorageUsed() ?: 0L
    }
    
    /**
     * Get count of downloaded trails.
     */
    suspend fun getDownloadCount(): Int {
        return downloadedTrailDao.getDownloadCount()
    }
}

private fun Trail.toCreateRequest(): CreateTrailRequest {
    return CreateTrailRequest(
        id = id,
        name = name,
        city = city ?: "Unknown",
        latitude = latitude,
        longitude = longitude,
        distanceKm = distanceKm ?: 0.0,
        elevationM = elevationM ?: 0,
        difficulty = (difficulty ?: Difficulty.MODERATE).name.lowercase(),
        rating = rating ?: 0.0,
        imageUrl = imageUrl,
        tags = tags.takeIf { it.isNotEmpty() },
        description = description
    )
}

