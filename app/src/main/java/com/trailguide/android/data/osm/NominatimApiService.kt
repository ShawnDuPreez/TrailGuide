package com.trailguide.android.data.osm

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for OpenStreetMap Nominatim API
 * Used to fetch nature reserve boundaries and OSM relation IDs
 * 
 * Nominatim Documentation: https://nominatim.org/release-docs/latest/api/Search/
 */
interface NominatimApiService {
    
    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/"
    }
    
    /**
     * Search for a place and get its boundary polygon
     * 
     * @param query Search term (e.g., "Faerie Glen Nature Reserve")
     * @param format Response format (json)
     * @param polygonGeoJson Include GeoJSON polygon in response
     * @param limit Max results
     * @return List of search results with boundaries
     */
    @GET("search")
    suspend fun searchPlace(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("polygon_geojson") polygonGeoJson: Int = 1,
        @Query("limit") limit: Int = 5
    ): Response<List<NominatimResult>>
    
    /**
     * Reverse geocode to find OSM object at coordinates
     * Used when clicking on Google Places result to find OSM boundary
     * 
     * @param lat Latitude
     * @param lon Longitude
     * @param format Response format
     * @param polygonGeoJson Include polygon
     * @param zoom Detail level (3-18, higher = more detailed)
     */
    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json",
        @Query("polygon_geojson") polygonGeoJson: Int = 1,
        @Query("zoom") zoom: Int = 18
    ): Response<NominatimResult>
}
