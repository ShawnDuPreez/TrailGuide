package com.trailguide.android.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * REST API service for offline sync endpoints.
 */
interface SyncApiService {
    
    /**
     * POST /api/sync - Sync offline data (reviews, favourites, activities)
     */
    @POST("api/sync")
    suspend fun syncOfflineData(@Body request: SyncRequest): Response<SyncResponse>
}

/**
 * Request body for sync endpoint.
 */
data class SyncRequest(
    val reviews: List<SyncReview>? = null,
    val favourites: List<SyncFavourite>? = null,
    val activities: List<SyncActivity>? = null
)

/**
 * Review data for sync.
 */
data class SyncReview(
    val trail_id: String,
    val user_name: String,
    val rating: Double,
    val comment: String,
    val photos: List<String> = emptyList(),
    val created_at: String? = null
)

/**
 * Favourite data for sync.
 */
data class SyncFavourite(
    val trail_id: String,
    val created_at: String? = null
)

/**
 * Activity data for sync.
 */
data class SyncActivity(
    val trail_id: String,
    val duration_minutes: Int? = null,
    val distance_km: Double? = null,
    val completed_at: String? = null
)

/**
 * Response from sync endpoint.
 */
data class SyncResponse(
    val status: String,
    val reviews_synced: Int = 0,
    val favourites_synced: Int = 0,
    val activities_synced: Int = 0,
    val errors: List<SyncError> = emptyList()
)

/**
 * Error from sync operation.
 */
data class SyncError(
    val type: String,
    val error: String
)

