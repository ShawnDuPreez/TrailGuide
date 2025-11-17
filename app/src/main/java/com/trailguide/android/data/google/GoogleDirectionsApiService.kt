package com.trailguide.android.data.google

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for Google Directions API.
 * Used for calculating routes and navigation for hiking trails.
 */
interface GoogleDirectionsApiService {
    
    /**
     * Get directions between two points.
     * 
     * @param origin Starting point as "lat,lng" or place_id:XXX
     * @param destination End point as "lat,lng" or place_id:XXX
     * @param mode Travel mode: driving, walking, bicycling, transit
     * @param waypoints Optional waypoints as "lat,lng|lat,lng" or "place_id:XXX|place_id:YYY"
     * @param alternatives If true, return alternative routes
     * @param key Google API key
     */
    @GET("directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "walking",
        @Query("waypoints") waypoints: String? = null,
        @Query("alternatives") alternatives: Boolean = false,
        @Query("key") key: String
    ): DirectionsResponse
    
    /**
     * Get circular/loop trail directions.
     * Creates a route that returns to the starting point.
     * 
     * @param origin Starting point as "lat,lng"
     * @param waypoints Points along the trail as "lat,lng|lat,lng"
     * @param mode Travel mode (walking for hiking)
     * @param key Google API key
     */
    @GET("directions/json")
    suspend fun getLoopTrail(
        @Query("origin") origin: String,
        @Query("destination") destination: String, // Same as origin for loop
        @Query("waypoints") waypoints: String,
        @Query("mode") mode: String = "walking",
        @Query("key") key: String
    ): DirectionsResponse
    
    companion object {
        const val BASE_URL = "https://maps.googleapis.com/maps/api/"
        
        /**
         * Travel modes for directions.
         */
        const val MODE_WALKING = "walking"
        const val MODE_BICYCLING = "bicycling"
        const val MODE_DRIVING = "driving"
        const val MODE_TRANSIT = "transit"
    }
}

