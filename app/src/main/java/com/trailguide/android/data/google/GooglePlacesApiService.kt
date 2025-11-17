package com.trailguide.android.data.google

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for Google Places API.
 * Used for discovering hiking trails and points of interest.
 */
interface GooglePlacesApiService {
    
    /**
     * Search for hiking trails using text query.
     * 
     * @param query Search query (e.g., "hiking trail", "hiking", "nature reserve")
     * @param location Center point as "lat,lng"
     * @param radius Search radius in meters (max 50000)
     * @param key Google API key
     * @param type Filter by type (e.g., "park", "point_of_interest")
     */
    @GET("place/textsearch/json")
    suspend fun searchHikingTrails(
        @Query("query") query: String,
        @Query("location") location: String? = null,
        @Query("radius") radius: Int? = null,
        @Query("key") key: String,
        @Query("type") type: String? = null
    ): PlacesSearchResponse
    
    /**
     * Search for nearby hiking trails.
     * 
     * @param location Center point as "lat,lng"
     * @param radius Search radius in meters (max 50000)
     * @param type Place type (e.g., "park", "point_of_interest", "tourist_attraction")
     * @param keyword Additional filtering keyword
     * @param key Google API key
     */
    @GET("place/nearbysearch/json")
    suspend fun nearbySearch(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("keyword") keyword: String? = null,
        @Query("key") key: String
    ): PlacesSearchResponse
    
    /**
     * Get detailed information about a specific place.
     * 
     * @param placeId Google Place ID
     * @param fields Comma-separated list of fields to return
     * @param key Google API key
     */
    @GET("place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "place_id,name,formatted_address,formatted_phone_number,geometry,rating,user_ratings_total,photos,reviews,website,opening_hours,types",
        @Query("key") key: String
    ): PlaceDetailsResponse
    
    /**
     * Get next page of search results.
     * Note: Must wait ~2 seconds after previous request before using pagetoken.
     * 
     * @param pageToken Token from previous search response
     * @param key Google API key
     */
    @GET("place/textsearch/json")
    suspend fun getNextPage(
        @Query("pagetoken") pageToken: String,
        @Query("key") key: String
    ): PlacesSearchResponse
    
    companion object {
        const val BASE_URL = "https://maps.googleapis.com/maps/api/"
        
        /**
         * Recommended search queries for hiking trails.
         */
        val HIKING_QUERIES = listOf(
            "hiking trail",
            "hiking",
            "nature trail",
            "nature reserve",
            "mountain trail",
            "forest trail",
            "scenic trail"
        )
        
        /**
         * Place types relevant to hiking.
         */
        val HIKING_TYPES = listOf(
            "park",
            "point_of_interest",
            "tourist_attraction",
            "natural_feature"
        )
    }
}

