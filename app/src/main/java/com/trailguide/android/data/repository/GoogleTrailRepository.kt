package com.trailguide.android.data.repository

import android.util.Log
import com.trailguide.android.BuildConfig
import com.trailguide.android.data.google.*
import com.trailguide.android.data.model.RoutePoint
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.model.TrailSegment
import com.trailguide.android.data.remote.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for accessing Google Places and Directions APIs.
 * Provides hiking trail discovery and navigation.
 */
@Singleton
class GoogleTrailRepository @Inject constructor(
    private val placesApiService: GooglePlacesApiService,
    private val directionsApiService: GoogleDirectionsApiService,
    private val elevationApiService: GoogleElevationApiService
) {
    
    companion object {
        private const val TAG = "GoogleTrailRepository"
        private const val DEFAULT_SEARCH_RADIUS = 50000 // 50km
    }
    
    /**
     * Search for hiking trails near a location.
     * 
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radius Search radius in meters (default 50km)
     * @param query Optional search query (default "hiking trail")
     */
    suspend fun searchHikingTrails(
        latitude: Double,
        longitude: Double,
        radius: Int = DEFAULT_SEARCH_RADIUS,
        query: String = "hiking trail"
    ): Flow<NetworkResult<List<Trail>>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Searching hiking trails near ($latitude, $longitude) with radius $radius")
            
            val location = "$latitude,$longitude"
            
            // Try text search first for better results
            val response = placesApiService.searchHikingTrails(
                query = query,
                location = location,
                radius = radius,
                key = BuildConfig.GOOGLE_MAPS_API_KEY
            )
            
            if (response.status == "OK" || response.status == "ZERO_RESULTS") {
                val trails = response.results
                    .filter { isHikingRelated(it) }
                    .map { it.toTrail() }
                
                emit(NetworkResult.Success(trails))
                Log.d(TAG, "Found ${trails.size} hiking trails")
            } else {
                emit(NetworkResult.Error("Search failed: ${response.status} - ${response.errorMessage}"))
                Log.e(TAG, "Places API error: ${response.status} - ${response.errorMessage}")
            }
            
        } catch (e: Exception) {
            emit(NetworkResult.Error("Failed to search trails: ${e.message}", e))
            Log.e(TAG, "Search error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search for nearby hiking trails using proximity search.
     */
    suspend fun nearbyHikingTrails(
        latitude: Double,
        longitude: Double,
        radius: Int = DEFAULT_SEARCH_RADIUS
    ): Flow<NetworkResult<List<Trail>>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Searching nearby hiking trails")
            
            val location = "$latitude,$longitude"
            
            // Use nearby search with hiking-related types
            val response = placesApiService.nearbySearch(
                location = location,
                radius = radius,
                type = "park",
                keyword = "hiking",
                key = BuildConfig.GOOGLE_MAPS_API_KEY
            )
            
            if (response.status == "OK" || response.status == "ZERO_RESULTS") {
                val trails = response.results
                    .filter { isHikingRelated(it) }
                    .map { it.toTrail() }
                
                emit(NetworkResult.Success(trails))
                Log.d(TAG, "Found ${trails.size} nearby trails")
            } else {
                emit(NetworkResult.Error("Nearby search failed: ${response.status}"))
                Log.e(TAG, "Nearby API error: ${response.status}")
            }
            
        } catch (e: Exception) {
            emit(NetworkResult.Error("Failed to search nearby: ${e.message}", e))
            Log.e(TAG, "Nearby search error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get detailed information about a trail by Place ID.
     */
    suspend fun getTrailDetails(placeId: String): Flow<NetworkResult<Trail>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Fetching details for place: $placeId")
            
            val response = placesApiService.getPlaceDetails(
                placeId = placeId,
                key = BuildConfig.GOOGLE_MAPS_API_KEY
            )
            
            if (response.status == "OK") {
                val trail = response.result.toTrail()
                emit(NetworkResult.Success(trail))
                Log.d(TAG, "Got trail details: ${trail.name}")
            } else {
                emit(NetworkResult.Error("Failed to get details: ${response.status}"))
                Log.e(TAG, "Place details error: ${response.status}")
            }
            
        } catch (e: Exception) {
            emit(NetworkResult.Error("Failed to get trail details: ${e.message}", e))
            Log.e(TAG, "Details error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get directions for a trail.
     */
    suspend fun getTrailDirections(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        waypoints: List<Pair<Double, Double>>? = null
    ): Flow<NetworkResult<DirectionRoute>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Getting directions from ($startLat, $startLng) to ($endLat, $endLng)")
            
            val origin = "$startLat,$startLng"
            val destination = "$endLat,$endLng"
            val waypointsStr = waypoints?.joinToString("|") { "${it.first},${it.second}" }
            
            val response = directionsApiService.getDirections(
                origin = origin,
                destination = destination,
                mode = GoogleDirectionsApiService.MODE_WALKING,
                waypoints = waypointsStr,
                alternatives = true,
                key = BuildConfig.GOOGLE_MAPS_API_KEY
            )
            
            if (response.status == "OK" && response.routes.isNotEmpty()) {
                val route = response.routes.first()
                emit(NetworkResult.Success(route))
                Log.d(TAG, "Got directions: ${route.summary}")
            } else {
                emit(NetworkResult.Error("No routes found: ${response.status}"))
                Log.e(TAG, "Directions error: ${response.status}")
            }
            
        } catch (e: Exception) {
            emit(NetworkResult.Error("Failed to get directions: ${e.message}", e))
            Log.e(TAG, "Directions error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Filter places that are hiking-related.
     */
    private fun isHikingRelated(place: PlaceResult): Boolean {
        val hikingKeywords = listOf(
            "trail", "hiking", "hike", "nature", "mountain", 
            "forest", "park", "scenic", "walk", "trek"
        )
        
        val nameContainsHiking = hikingKeywords.any { 
            place.name.contains(it, ignoreCase = true) 
        }
        
        val vicinityContainsHiking = place.vicinity?.let { vicinity ->
            hikingKeywords.any { vicinity.contains(it, ignoreCase = true) }
        } ?: false
        
        val typesContainHiking = place.types.any { type ->
            type in listOf("park", "point_of_interest", "tourist_attraction", "natural_feature")
        }
        
        return nameContainsHiking || vicinityContainsHiking || typesContainHiking
    }
    
    /**
     * Get complete trail details with ALL stats auto-fetched.
     * This is the main method that pulls distance, elevation, segments, etc.
     * 
     * @param placeId Google Place ID
     * @param fetchDirections Whether to fetch full directions/route (slower)
     */
    suspend fun getCompleteTrailDetails(
        placeId: String,
        fetchDirections: Boolean = true
    ): Flow<NetworkResult<Trail>> = flow {
        emit(NetworkResult.Loading)
        
        try {
            Log.d(TAG, "Fetching complete trail details for: $placeId")
            
            // Step 1: Get basic place details
            val placeResponse = placesApiService.getPlaceDetails(
                placeId = placeId,
                key = BuildConfig.GOOGLE_MAPS_API_KEY
            )
            
            if (placeResponse.status != "OK") {
                emit(NetworkResult.Error("Failed to get place details: ${placeResponse.status}"))
                return@flow
            }
            
            var trail = placeResponse.result.toTrailComplete()
            
            // Step 2: If requested, fetch directions to calculate distance and route
            if (fetchDirections) {
                try {
                    // Create a loop trail or use nearby point
                    val lat = trail.latitude
                    val lng = trail.longitude
                    val nearbyLat = lat + 0.01 // ~1km offset
                    val nearbyLng = lng + 0.01
                    
                    val dirResponse = directionsApiService.getDirections(
                        origin = "$lat,$lng",
                        destination = "$lat,$lng", // Loop back
                        waypoints = "$nearbyLat,$nearbyLng",
                        mode = GoogleDirectionsApiService.MODE_WALKING,
                        key = BuildConfig.GOOGLE_MAPS_API_KEY
                    )
                    
                    if (dirResponse.status == "OK" && dirResponse.routes.isNotEmpty()) {
                        val route = dirResponse.routes.first()
                        
                        // Extract distance
                        val totalDistance = route.legs.sumOf { it.distance.value }.toDouble() / 1000.0
                        
                        // Extract duration
                        val totalDuration = route.legs.sumOf { it.duration.value }
                        val durationText = "${totalDuration / 3600}h ${(totalDuration % 3600) / 60}m"
                        
                        // Extract route coordinates from polyline
                        val routeCoords = route.overviewPolyline.decode().map {
                            RoutePoint(it.lat, it.lng)
                        }
                        
                        // Generate segments from steps
                        val segments = generateSegmentsFromRoute(route)
                        
                        trail = trail.copy(
                            distanceKm = totalDistance,
                            duration = durationText,
                            routeCoordinates = routeCoords,
                            segments = segments
                        )
                        
                        // Step 3: Fetch elevation data for the route
                        if (routeCoords.isNotEmpty()) {
                            try {
                                val elevationResponse = elevationApiService.getElevationAlongPath(
                                    path = route.overviewPolyline.points,
                                    samples = 50,
                                    key = BuildConfig.GOOGLE_MAPS_API_KEY
                                )
                                
                                if (elevationResponse.status == "OK") {
                                    val elevationGain = elevationResponse.results.calculateElevationGain()
                                    
                                    trail = trail.copy(
                                        elevationM = elevationGain.toInt()
                                    )
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to fetch elevation: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch directions: ${e.message}")
                }
            }
            
            emit(NetworkResult.Success(trail))
            Log.d(TAG, "Complete trail details loaded: ${trail.name}")
            
        } catch (e: Exception) {
            emit(NetworkResult.Error("Failed to load complete trail: ${e.message}", e))
            Log.e(TAG, "Complete trail error", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Generate trail segments from directions route.
     */
    private fun generateSegmentsFromRoute(route: DirectionRoute): List<TrailSegment> {
        return route.legs.flatMap { leg ->
            leg.steps.mapIndexed { index, step ->
                val segmentName = if (index == 0) {
                    "Trailhead → ${index + 1}"
                } else if (index == leg.steps.size - 1) {
                    "${index} → Summit"
                } else {
                    "${index} → ${index + 1}"
                }
                
                // Infer segment type from instructions
                val type = when {
                    step.htmlInstructions.contains("steep", ignoreCase = true) -> "Steep"
                    step.htmlInstructions.contains("climb", ignoreCase = true) -> "Climb"
                    step.htmlInstructions.contains("exposed", ignoreCase = true) -> "Exposed"
                    step.htmlInstructions.contains("easy", ignoreCase = true) -> "Easy"
                    else -> "Moderate"
                }
                
                TrailSegment(
                    name = segmentName,
                    description = step.htmlInstructions.replace("<[^>]*>".toRegex(), ""),
                    type = type,
                    distance = step.distance.value.toDouble(),
                    duration = step.duration.value,
                    startPoint = RoutePoint(step.startLocation.lat, step.startLocation.lng),
                    endPoint = RoutePoint(step.endLocation.lat, step.endLocation.lng)
                )
            }
        }
    }
    
    /**
     * Get photo URL for a place.
     */
    fun getPhotoUrl(photoReference: String, maxWidth: Int = 400): String {
        return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=$maxWidth&photo_reference=$photoReference&key=${BuildConfig.GOOGLE_MAPS_API_KEY}"
    }
}

/**
 * Extension function to convert PlaceResult to Trail model (basic).
 */
private fun PlaceResult.toTrail(): Trail {
    val photoUrl = photos?.firstOrNull()?.let { photo ->
        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference=${photo.photoReference}&key=${BuildConfig.GOOGLE_MAPS_API_KEY}"
    }
    
    return Trail(
        id = placeId,
        name = name,
        city = vicinity,
        latitude = geometry.location.lat,
        longitude = geometry.location.lng,
        description = vicinity ?: "Hiking trail near $name",
        imageUrl = photoUrl,
        rating = rating,
        reviewCount = userRatingsTotal,
        isFavorite = false
    )
}

/**
 * Extension function to convert PlaceDetails to Trail model (basic).
 */
private fun PlaceDetails.toTrail(): Trail {
    val photoUrl = photos?.firstOrNull()?.let { photo ->
        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference=${photo.photoReference}&key=${BuildConfig.GOOGLE_MAPS_API_KEY}"
    }
    
    return Trail(
        id = placeId,
        name = name,
        city = formattedAddress,
        latitude = geometry.location.lat,
        longitude = geometry.location.lng,
        description = formattedAddress ?: "Hiking trail at $name",
        imageUrl = photoUrl,
        rating = rating,
        reviewCount = userRatingsTotal,
        isFavorite = false
    )
}

/**
 * Extension function to convert PlaceDetails to complete Trail model with all fields.
 */
private fun PlaceDetails.toTrailComplete(): Trail {
    val photoUrl = photos?.firstOrNull()?.let { photo ->
        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference=${photo.photoReference}&key=${BuildConfig.GOOGLE_MAPS_API_KEY}"
    }
    
    // Extract description from reviews or formatted address
    val description = reviews?.firstOrNull()?.text 
        ?: formattedAddress 
        ?: "Hiking trail at $name"
    
    return Trail(
        id = placeId,
        name = name,
        city = formattedAddress?.substringAfter(",")?.trim(),
        latitude = geometry.location.lat,
        longitude = geometry.location.lng,
        description = description,
        imageUrl = photoUrl,
        rating = rating,
        reviewCount = userRatingsTotal,
        formattedAddress = formattedAddress,
        website = website,
        phoneNumber = formattedPhoneNumber,
        isFavorite = false
    )
}

