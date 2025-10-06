package com.trailguide.android.data.remote

import com.trailguide.android.data.dto.ApiResponse
import com.trailguide.android.data.dto.CreateTrailRequest
import com.trailguide.android.data.dto.TrailDto
import retrofit2.Response
import retrofit2.http.*

/**
 * REST API service for trail-related endpoints.
 * This interfaces with our Node.js/Express proxy that connects to Supabase.
 */
interface TrailApiService {
    
    /**
     * GET /api/trails - Fetch all trails
     */
    @GET("api/trails")
    suspend fun getAllTrails(): Response<List<TrailDto>>
    
    /**
     * GET /api/trails/{id} - Fetch a specific trail by ID
     */
    @GET("api/trails/{id}")
    suspend fun getTrailById(@Path("id") trailId: String): Response<TrailDto>
    
    /**
     * POST /api/trails - Create a new trail
     */
    @POST("api/trails")
    suspend fun createTrail(@Body request: CreateTrailRequest): Response<TrailDto>
    
    /**
     * PUT /api/trails/{id} - Update an existing trail
     */
    @PUT("api/trails/{id}")
    suspend fun updateTrail(
        @Path("id") trailId: String,
        @Body request: CreateTrailRequest
    ): Response<TrailDto>
    
    /**
     * DELETE /api/trails/{id} - Delete a trail
     */
    @DELETE("api/trails/{id}")
    suspend fun deleteTrail(@Path("id") trailId: String): Response<Unit>
    
    /**
     * GET /api/trails/search - Search trails by query
     */
    @GET("api/trails/search")
    suspend fun searchTrails(
        @Query("q") query: String,
        @Query("difficulty") difficulty: String? = null,
        @Query("maxDistance") maxDistance: Double? = null
    ): Response<List<TrailDto>>
    
    /**
     * POST /api/users/{userId}/favourites - Add trail to favorites
     */
    @POST("api/users/{userId}/favourites")
    suspend fun addFavorite(
        @Path("userId") userId: String,
        @Body body: Map<String, String>
    ): Response<Map<String, String>>
    
    /**
     * DELETE /api/users/{userId}/favourites/{trailId} - Remove trail from favorites
     */
    @DELETE("api/users/{userId}/favourites/{trailId}")
    suspend fun removeFavorite(
        @Path("userId") userId: String,
        @Path("trailId") trailId: String
    ): Response<Map<String, String>>
    
    /**
     * GET /api/users/{userId}/favourites - Get user's favorite trails
     */
    @GET("api/users/{userId}/favourites")
    suspend fun getFavoriteTrails(
        @Path("userId") userId: String
    ): Response<List<TrailDto>>
    
    /**
     * GET /api/health - Health check to wake up server (Render.com cold start)
     */
    @GET("api/health")
    suspend fun healthCheck(): Response<Unit>
}

