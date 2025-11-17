package com.trailguide.android.data.google

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for Google Elevation API.
 * Used for fetching elevation data for hiking trails.
 */
interface GoogleElevationApiService {
    
    /**
     * Get elevation for a list of locations.
     * 
     * @param locations Pipe-separated lat,lng values (e.g., "39.7391536,-104.9847034|36.455556,-116.866667")
     * @param key Google API key
     */
    @GET("elevation/json")
    suspend fun getElevation(
        @Query("locations") locations: String,
        @Query("key") key: String
    ): ElevationResponse
    
    /**
     * Get elevation along a path (polyline).
     * Samples elevation at regular intervals along the path.
     * 
     * @param path Encoded polyline or pipe-separated lat,lng values
     * @param samples Number of sample points (max 512)
     * @param key Google API key
     */
    @GET("elevation/json")
    suspend fun getElevationAlongPath(
        @Query("path") path: String,
        @Query("samples") samples: Int = 100,
        @Query("key") key: String
    ): ElevationResponse
    
    companion object {
        const val BASE_URL = "https://maps.googleapis.com/maps/api/"
        const val MAX_SAMPLES = 512
        const val DEFAULT_SAMPLES = 100
    }
}

