package com.trailguide.android.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.trailguide.android.data.osm.*
import com.trailguide.android.data.remote.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching and managing OSM hiking trails
 * Uses Overpass API to get real trail geometry from OpenStreetMap
 * 
 * Features:
 * - Fetch trails near a location
 * - Search trails by name
 * - Get trail details by ID
 * - Filter by difficulty and type
 * - Cache and offline support (future)
 */
@Singleton
class OsmTrailRepository @Inject constructor(
    private val overpassApiService: OverpassApiService
) {
    
    companion object {
        private const val TAG = "OsmTrailRepository"
        private const val DEFAULT_SEARCH_RADIUS = 5000 // 5km
        private const val MAX_SEARCH_RADIUS = 20000 // 20km
    }
    
    /**
     * Fetch hiking trails near a location
     * 
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radius Search radius in meters
     * @return Flow of NetworkResult with list of OSM trails
     */
    fun fetchTrailsNearby(
        latitude: Double,
        longitude: Double,
        radius: Int = DEFAULT_SEARCH_RADIUS
    ): Flow<NetworkResult<List<OsmTrail>>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "======================================")
            Log.d(TAG, "Fetching OSM trails near ($latitude, $longitude) within ${radius}m")
            
            // Build Overpass query
            val query = OverpassApiService.buildHikingTrailsQuery(
                lat = latitude,
                lng = longitude,
                radius = radius.coerceAtMost(MAX_SEARCH_RADIUS)
            )
            
            Log.d(TAG, "Overpass query:")
            Log.d(TAG, query)
            Log.d(TAG, "======================================")
            
            // Execute API call
            val response = overpassApiService.executeQuery(query)
            
            if (!response.isSuccessful) {
                emit(NetworkResult.Error("Overpass API error: ${response.code()} ${response.message()}"))
                return@flow
            }
            
            val overpassResponse = response.body()
            if (overpassResponse == null) {
                emit(NetworkResult.Error("Empty response from Overpass API"))
                return@flow
            }
            
            Log.d(TAG, "Received ${overpassResponse.elements.size} OSM elements")
            
            // Convert OSM elements to OsmTrail models
            val trails = parseTrailsFromResponse(overpassResponse)
            
            Log.d(TAG, "Parsed ${trails.size} valid hiking trails")
            
            if (trails.isEmpty()) {
                emit(NetworkResult.Success(emptyList()))
            } else {
                // Sort by distance from search center and limit to 50 results
                val sortedTrails = trails.sortedBy { trail ->
                    trail.startPoint?.let { start ->
                        calculateDistance(
                            LatLng(latitude, longitude),
                            start
                        )
                    } ?: Double.MAX_VALUE
                }.take(50) // Limit to 50 trails to avoid rate limits and UI overload
                
                Log.d(TAG, "Returning ${sortedTrails.size} trails (limited to 50)")
                emit(NetworkResult.Success(sortedTrails))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching OSM trails", e)
            emit(NetworkResult.Error("Failed to fetch trails: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search for trails by name
     * 
     * @param searchQuery Trail name to search for
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radius Search radius in meters
     * @return Flow of NetworkResult with matching trails
     */
    fun searchTrails(
        searchQuery: String,
        latitude: Double,
        longitude: Double,
        radius: Int = DEFAULT_SEARCH_RADIUS * 2
    ): Flow<NetworkResult<List<OsmTrail>>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Searching for trails: \"$searchQuery\" near ($latitude, $longitude)")
            
            val query = OverpassApiService.buildTrailSearchQuery(
                searchQuery = searchQuery,
                lat = latitude,
                lng = longitude,
                radius = radius.coerceAtMost(MAX_SEARCH_RADIUS)
            )
            
            val response = overpassApiService.executeQuery(query)
            
            if (!response.isSuccessful) {
                emit(NetworkResult.Error("Search failed: ${response.message()}"))
                return@flow
            }
            
            val overpassResponse = response.body()
            if (overpassResponse == null) {
                emit(NetworkResult.Error("Empty search response"))
                return@flow
            }
            
            val trails = parseTrailsFromResponse(overpassResponse)
            
            Log.d(TAG, "Found ${trails.size} trails matching \"$searchQuery\"")
            
            emit(NetworkResult.Success(trails))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching trails", e)
            emit(NetworkResult.Error("Search failed: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Fetch a specific trail by OSM ID
     * 
     * @param osmId OpenStreetMap way ID
     * @return Flow of NetworkResult with the trail
     */
    fun fetchTrailById(osmId: Long): Flow<NetworkResult<OsmTrail>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Fetching trail with OSM ID: $osmId")
            
            val query = OverpassApiService.buildTrailByIdQuery(osmId)
            val response = overpassApiService.executeQuery(query)
            
            if (!response.isSuccessful) {
                emit(NetworkResult.Error("Failed to fetch trail: ${response.message()}"))
                return@flow
            }
            
            val overpassResponse = response.body()
            if (overpassResponse == null || overpassResponse.elements.isEmpty()) {
                emit(NetworkResult.Error("Trail not found"))
                return@flow
            }
            
            val trails = parseTrailsFromResponse(overpassResponse)
            val trail = trails.firstOrNull()
            
            if (trail == null) {
                emit(NetworkResult.Error("Trail not found or invalid"))
            } else {
                Log.d(TAG, "Successfully fetched trail: ${trail.name}")
                emit(NetworkResult.Success(trail))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trail by ID", e)
            emit(NetworkResult.Error("Failed to fetch trail: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Fetch trails with advanced filters
     * 
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radius Search radius in meters
     * @param difficulty Minimum difficulty filter
     * @param trailTypes List of trail types to include
     * @return Flow of NetworkResult with filtered trails
     */
    fun fetchTrailsWithFilters(
        latitude: Double,
        longitude: Double,
        radius: Int = DEFAULT_SEARCH_RADIUS,
        difficulty: TrailDifficulty? = null,
        trailTypes: List<TrailType>? = null
    ): Flow<NetworkResult<List<OsmTrail>>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            val difficultyStr = when (difficulty) {
                TrailDifficulty.EASY -> "easy"
                TrailDifficulty.MODERATE -> "moderate"
                TrailDifficulty.DIFFICULT, TrailDifficulty.EXPERT -> "difficult"
                else -> null
            }
            
            val typeStrs = trailTypes?.map { it.osmValue }
            
            val query = OverpassApiService.buildAdvancedTrailsQuery(
                lat = latitude,
                lng = longitude,
                radius = radius,
                minDifficulty = difficultyStr,
                trailTypes = typeStrs
            )
            
            val response = overpassApiService.executeQuery(query)
            
            if (!response.isSuccessful) {
                emit(NetworkResult.Error("Failed to fetch trails: ${response.message()}"))
                return@flow
            }
            
            val overpassResponse = response.body()
            if (overpassResponse == null) {
                emit(NetworkResult.Error("Empty response"))
                return@flow
            }
            
            val trails = parseTrailsFromResponse(overpassResponse)
            
            emit(NetworkResult.Success(trails))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching filtered trails", e)
            emit(NetworkResult.Error("Failed to fetch trails: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Parse Overpass API response into OsmTrail models
     * 
     * @param response Overpass API response
     * @return List of valid OsmTrail objects
     */
    private fun parseTrailsFromResponse(response: OverpassResponse): List<OsmTrail> {
        val trails = mutableListOf<OsmTrail>()
        
        // First pass: collect all nodes for reference
        val nodeMap = mutableMapOf<Long, LatLng>()
        response.elements
            .filter { it.type == "node" && it.lat != null && it.lon != null }
            .forEach { node ->
                nodeMap[node.id] = LatLng(node.lat!!, node.lon!!)
            }
        
        // Second pass: process ways (trails)
        response.elements
            .filter { it.type == "way" }
            .forEach { way ->
                try {
                    val trail = way.toOsmTrail(nodeMap)
                    if (trail != null && trail.geometry.size >= 2) {
                        trails.add(trail)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse trail ${way.id}: ${e.message}")
                }
            }
        
        return trails
    }
    
    /**
     * Calculate distance between two LatLng points using Haversine formula
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLon = Math.toRadians(point2.longitude - point1.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}
