package com.trailguide.android.data.remote

import com.trailguide.android.data.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * REST API service for authentication endpoints.
 */
interface AuthApiService {
    
    /**
     * POST /api/auth/login - Login with email/password
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<OAuthTokenResponse>
    
    /**
     * POST /api/auth/register - Register new user
     */
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<OAuthTokenResponse>
    
    /**
     * POST /api/auth/logout - Logout current user
     */
    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>
    
    /**
     * GET /api/auth/user - Get current authenticated user
     */
    @GET("api/auth/user")
    suspend fun getCurrentUser(): Response<UserDto>
    
    /**
     * POST /api/auth/refresh - Refresh access token
     */
    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body refreshToken: Map<String, String>): Response<OAuthTokenResponse>
}

