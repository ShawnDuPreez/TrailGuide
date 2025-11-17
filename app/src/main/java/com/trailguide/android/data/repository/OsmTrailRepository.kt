package com.trailguide.android.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.trailguide.android.data.osm.*
import com.trailguide.android.data.remote.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
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
    private val overpassApiService: OverpassApiService,
    private val nominatimApiService: NominatimApiService
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
    
    // ===== BOUNDARY-BASED TRAIL QUERIES =====
    
    /**
     * Search for a place boundary by name (for nature reserves, parks, etc.)
     * Uses Nominatim to find OSM boundaries
     * 
     * @param placeName Name of the place to search
     * @return Flow of NetworkResult with boundary data
     */
    fun searchPlaceBoundary(placeName: String): Flow<NetworkResult<OsmBoundary>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Searching for place boundary: $placeName")
            
            val response = nominatimApiService.searchPlace(query = placeName)
            
            if (!response.isSuccessful || response.body() == null) {
                emit(NetworkResult.Error("Failed to find place: ${response.message()}"))
                return@flow
            }
            
            val results = response.body()!!
            if (results.isEmpty()) {
                emit(NetworkResult.Error("No results found for: $placeName"))
                return@flow
            }
            
            // Prefer relations (they're usually parks/reserves), then ways, then nodes
            val bestResult = results.firstOrNull { it.osmType == "relation" }
                ?: results.firstOrNull { it.osmType == "way" }
                ?: results.first()
            
            val boundary = parseBoundaryFromNominatim(bestResult)
            
            Log.d(TAG, "Found boundary: ${boundary.name} (OSM ${boundary.osmType} ${boundary.osmId})")
            emit(NetworkResult.Success(boundary))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching place boundary", e)
            emit(NetworkResult.Error("Search failed: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get boundary for a specific location using name search + proximity verification
     * Much more accurate than reverse geocoding for nature reserves
     * 
     * @param placeName Name of the place from Google Places
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @return Flow of NetworkResult with boundary data
     */
    fun getBoundaryByNameAndLocation(
        placeName: String,
        latitude: Double,
        longitude: Double
    ): Flow<NetworkResult<OsmBoundary>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Searching for boundary: '$placeName' near ($latitude, $longitude)")
            
            // Search by name first (more accurate for specific places)
            val response = nominatimApiService.searchPlace(query = placeName)
            
            if (!response.isSuccessful || response.body() == null) {
                Log.w(TAG, "Name search failed: ${response.message()}")
                emit(NetworkResult.Error("Could not find boundary for $placeName"))
                return@flow
            }
            
            val results = response.body()!!
            if (results.isEmpty()) {
                Log.w(TAG, "No results found for: $placeName")
                emit(NetworkResult.Error("No boundary found"))
                return@flow
            }
            
            Log.d(TAG, "Found ${results.size} results for '$placeName'")
            
            // Find the result closest to the clicked location
            val sortedResults = results
                .map { result ->
                    val resultLat = result.lat.toDoubleOrNull() ?: 0.0
                    val resultLon = result.lon.toDoubleOrNull() ?: 0.0
                    val distance = calculateDistanceMeters(
                        latitude, longitude,
                        resultLat, resultLon
                    )
                    Pair(result, distance)
                }
                .sortedBy { it.second } // Closest first
            
            // Take the closest result that's within 5km
            val closestMatch = sortedResults.firstOrNull { it.second < 5000 }
            
            if (closestMatch == null) {
                Log.w(TAG, "No results within 5km of clicked location")
                emit(NetworkResult.Error("No nearby boundary found"))
                return@flow
            }
            
            val (result, distance) = closestMatch
            val boundary = parseBoundaryFromNominatim(result)
            
            Log.d(TAG, "✓ Using: ${boundary.name} (${distance.toInt()}m away, ${boundary.polygon.size} polygon points)")
            
            // If polygon is empty, this isn't useful
            if (boundary.polygon.isEmpty()) {
                Log.w(TAG, "Selected boundary has no polygon, using bounding box fallback")
                emit(NetworkResult.Error("No polygon available"))
                return@flow
            }
            
            emit(NetworkResult.Success(boundary))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting boundary by name", e)
            emit(NetworkResult.Error("Search failed: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Calculate distance in meters between two lat/lon points
     */
    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
    
    /**
     * Fetch all hiking trails within a specific boundary
     * This is THE KEY FUNCTION for showing trails inside nature reserves
     * 
     * @param boundary The OSM boundary to search within
     * @return Flow of NetworkResult with trails inside the boundary
     */
    fun fetchTrailsInBoundary(
        boundary: OsmBoundary
    ): Flow<NetworkResult<List<OsmTrail>>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "Fetching trails in boundary: ${boundary.name}")
            Log.d(TAG, "OSM ${boundary.osmType} ${boundary.osmId}")
            Log.d(TAG, "Center: (${boundary.centerLat}, ${boundary.centerLon})")
            Log.d(TAG, "Polygon points: ${boundary.polygon.size}")
            
            // Special handling for bounding box fallback (no real OSM ID)
            if (boundary.osmType == "bbox") {
                Log.d(TAG, "Using bounding box search (no OSM boundary found)")
                val bbox = boundary.boundingBox!!
                val bboxQuery = OverpassApiService.buildBBoxTrailsQuery(
                    south = bbox.south,
                    west = bbox.west,
                    north = bbox.north,
                    east = bbox.east
                )
                
                Log.d(TAG, "BBox Query: [${'$'}{bbox.south},${'$'}{bbox.west},${'$'}{bbox.north},${'$'}{bbox.east}]")
                val response = overpassApiService.executeQuery(bboxQuery)
                
                if (response.isSuccessful && response.body() != null) {
                    val trails = parseTrailsFromResponse(response.body()!!)
                    Log.d(TAG, "✓ Found ${trails.size} trails via bounding box")
                    emit(NetworkResult.Success(trails))
                } else {
                    Log.e(TAG, "✗ Bounding box query failed: ${response.message()}")
                    emit(NetworkResult.Error("No trails found in area"))
                }
                return@flow
            }
            
            // Try area-based query first (for real OSM boundaries)
            Log.d(TAG, "Building area query for ${boundary.osmType} ${boundary.osmId}")
            val query = OverpassApiService.buildBoundaryTrailsQuery(
                osmId = boundary.osmId,
                osmType = boundary.osmType
            )
            
            Log.d(TAG, "Executing Overpass area query...")
            var response = overpassApiService.executeQuery(query)
            var trails: List<OsmTrail> = emptyList()
            
            // If area query fails, fallback to bounding box
            if (!response.isSuccessful) {
                Log.w(TAG, "Area query failed: ${response.message()}")
                
                if (boundary.boundingBox != null) {
                    Log.d(TAG, "Trying bounding box fallback...")
                    val bbox = boundary.boundingBox
                    val bboxQuery = OverpassApiService.buildBBoxTrailsQuery(
                        south = bbox.south,
                        west = bbox.west,
                        north = bbox.north,
                        east = bbox.east
                    )
                    response = overpassApiService.executeQuery(bboxQuery)
                    
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Bounding box fallback also failed")
                    }
                } else {
                    Log.w(TAG, "No bounding box available for fallback")
                }
            }
            
            if (!response.isSuccessful) {
                emit(NetworkResult.Error("Failed to fetch trails: ${response.message()}"))
                return@flow
            }
            
            val overpassResponse = response.body()
            if (overpassResponse != null) {
                trails = parseTrailsFromResponse(overpassResponse)
                Log.d(TAG, "✓ Successfully parsed ${trails.size} trails in ${boundary.name}")
                trails.forEachIndexed { index, trail ->
                    Log.d(TAG, "  Trail ${index + 1}: ${trail.name} (${trail.geometry.size} points, ${String.format("%.2f", trail.distance / 1000)}km)")
                }
            } else {
                Log.w(TAG, "Empty response body from Overpass")
            }
            
            Log.d(TAG, "========================================")
            emit(NetworkResult.Success(trails))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching boundary trails", e)
            emit(NetworkResult.Error("Failed to fetch trails: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Parse Nominatim result into OsmBoundary
     */
    private fun parseBoundaryFromNominatim(result: NominatimResult): OsmBoundary {
        val centerLat = result.lat.toDoubleOrNull() ?: 0.0
        val centerLon = result.lon.toDoubleOrNull() ?: 0.0
        
        // Parse polygon from GeoJSON
        val polygon = parseGeoJsonPolygon(result.geoJson)
        
        // Parse bounding box
        val boundingBox = result.boundingBox?.let { bbox ->
            if (bbox.size >= 4) {
                BoundingBox(
                    south = bbox[0].toDoubleOrNull() ?: 0.0,
                    north = bbox[1].toDoubleOrNull() ?: 0.0,
                    west = bbox[2].toDoubleOrNull() ?: 0.0,
                    east = bbox[3].toDoubleOrNull() ?: 0.0
                )
            } else null
        }
        
        return OsmBoundary(
            osmId = result.osmId,
            osmType = result.osmType,
            name = result.displayName,
            centerLat = centerLat,
            centerLon = centerLon,
            polygon = polygon,
            boundingBox = boundingBox
        )
    }
    
    /**
     * Parse GeoJSON polygon to List<LatLng>
     */
    private fun parseGeoJsonPolygon(geoJson: GeoJsonGeometry?): List<LatLng> {
        if (geoJson == null) return emptyList()
        
        return try {
            when (geoJson.type) {
                "Polygon" -> {
                    // Parse coordinates array
                    val jsonArray = geoJson.coordinates
                    val coords = parsePolygonCoordinates(jsonArray)
                    coords
                }
                "MultiPolygon" -> {
                    // Take first polygon
                    val jsonArray = geoJson.coordinates
                    val coords = parseMultiPolygonCoordinates(jsonArray)
                    coords
                }
                "Point" -> {
                    // Single point, create small polygon around it
                    emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse GeoJSON polygon: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Parse polygon coordinates from JSON
     */
    private fun parsePolygonCoordinates(jsonElement: kotlinx.serialization.json.JsonElement): List<LatLng> {
        val coords = mutableListOf<LatLng>()
        try {
            val outerRing = jsonElement.jsonArray[0]
            outerRing.jsonArray.forEach { point ->
                val arr = point.jsonArray
                if (arr.size >= 2) {
                    val lon = arr[0].jsonPrimitive.double
                    val lat = arr[1].jsonPrimitive.double
                    coords.add(LatLng(lat, lon))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing polygon coordinates: ${e.message}")
        }
        return coords
    }
    
    /**
     * Parse multi-polygon coordinates from JSON
     */
    private fun parseMultiPolygonCoordinates(jsonElement: kotlinx.serialization.json.JsonElement): List<LatLng> {
        val coords = mutableListOf<LatLng>()
        try {
            val firstPolygon = jsonElement.jsonArray[0]
            val outerRing = firstPolygon.jsonArray[0]
            outerRing.jsonArray.forEach { point ->
                val arr = point.jsonArray
                if (arr.size >= 2) {
                    val lon = arr[0].jsonPrimitive.double
                    val lat = arr[1].jsonPrimitive.double
                    coords.add(LatLng(lat, lon))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing multi-polygon coordinates: ${e.message}")
        }
        return coords
    }
}
