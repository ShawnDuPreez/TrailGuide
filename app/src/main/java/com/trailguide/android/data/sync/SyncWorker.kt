package com.trailguide.android.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trailguide.android.data.local.*
import com.trailguide.android.data.remote.ApiClient
import com.trailguide.android.data.remote.SyncApiService
import com.trailguide.android.data.remote.SyncFavourite
import com.trailguide.android.data.remote.SyncReview
import com.trailguide.android.data.remote.SyncActivity
import com.trailguide.android.data.remote.SyncRequest
import com.trailguide.android.data.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * WorkManager worker for syncing local Room data with Supabase backend.
 * Handles offline-first sync for favorites, progress, reviews, and collections.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: TrailDatabase,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {
    
    private val syncApiService: SyncApiService = ApiClient.syncApiService
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "trail_guide_sync"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync...")
            
            // Check if user is authenticated
            if (!authRepository.isSignedIn()) {
                Log.d(TAG, "User not signed in, skipping sync")
                return@withContext Result.success()
            }
            
            val userId = authRepository.currentUser?.id ?: run {
                Log.w(TAG, "No user ID found")
                return@withContext Result.failure()
            }
            
            // Get auth token for API calls
            val authToken = ApiClient.authToken
            if (authToken == null) {
                Log.w(TAG, "No auth token available, skipping sync")
                return@withContext Result.failure()
            }
            
            // Collect all pending data
            val pendingReviews = collectPendingReviews(userId)
            val pendingFavourites = collectPendingFavourites(userId)
            val pendingActivities = collectPendingActivities(userId)
            
            // Sync all data in one API call
            if (pendingReviews.isNotEmpty() || pendingFavourites.isNotEmpty() || pendingActivities.isNotEmpty()) {
                syncAllData(pendingReviews, pendingFavourites, pendingActivities, userId)
            }
            
            Log.d(TAG, "Sync completed successfully")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            
            // Retry on failure (max 3 attempts)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    /**
     * Collect pending reviews for sync.
     */
    private suspend fun collectPendingReviews(userId: String): List<SyncReview> {
        val reviewDao = database.reviewDao()
        val allReviews = reviewDao.getAllReviews()
        val userReviews = allReviews.filter { 
            it.userId == userId && 
            (it.syncStatus == SyncStatus.PENDING.name || it.syncStatus == SyncStatus.FAILED.name)
        }
        
        return userReviews.map { review ->
            SyncReview(
                trail_id = review.trailId,
                user_name = review.userName,
                rating = review.rating,
                comment = review.comment,
                photos = review.photos,
                created_at = if (review.createdAt > 0) {
                    dateFormat.format(Date(review.createdAt))
                } else null
            )
        }
    }
    
    /**
     * Collect pending favourites for sync.
     */
    private suspend fun collectPendingFavourites(userId: String): List<SyncFavourite> {
        val favoriteDao = database.favoriteTrailDao()
        val pendingFavorites = favoriteDao.getFavoritesByStatuses(
            listOf(SyncStatus.PENDING.name, SyncStatus.FAILED.name)
        )
        
        return pendingFavorites.map { favorite ->
            SyncFavourite(
                trail_id = favorite.trailId,
                created_at = if (favorite.addedAt > 0) {
                    dateFormat.format(Date(favorite.addedAt))
                } else null
            )
        }
    }
    
    /**
     * Collect pending activities (trail progress/completions) for sync.
     */
    private suspend fun collectPendingActivities(userId: String): List<SyncActivity> {
        val progressDao = database.trailProgressDao()
        val pendingProgress = progressDao.getProgressByStatuses(
            listOf(SyncStatus.PENDING.name, SyncStatus.FAILED.name)
        )
        
        return pendingProgress.filter { it.isCompleted }.map { progress ->
            SyncActivity(
                trail_id = progress.trailId,
                duration_minutes = if (progress.startedAt > 0 && progress.completedAt != null) {
                    ((progress.completedAt - progress.startedAt) / 60000).toInt()
                } else null,
                distance_km = progress.distanceCoveredKm,
                completed_at = progress.completedAt?.let { dateFormat.format(Date(it)) }
            )
        }
    }
    
    /**
     * Sync all data to the backend API.
     */
    private suspend fun syncAllData(
        reviews: List<SyncReview>,
        favourites: List<SyncFavourite>,
        activities: List<SyncActivity>,
        userId: String
    ) {
        try {
            Log.d(TAG, "Syncing ${reviews.size} reviews, ${favourites.size} favourites, ${activities.size} activities")
            
            // Mark all items as syncing
            markItemsAsSyncing(reviews, favourites, activities, userId)
            
            // Call sync API
            val request = SyncRequest(
                reviews = if (reviews.isNotEmpty()) reviews else null,
                favourites = if (favourites.isNotEmpty()) favourites else null,
                activities = if (activities.isNotEmpty()) activities else null
            )
            
            val response = syncApiService.syncOfflineData(request)
            
            if (response.isSuccessful && response.body() != null) {
                val syncResponse = response.body()!!
                Log.d(TAG, "Sync response: ${syncResponse.reviews_synced} reviews, ${syncResponse.favourites_synced} favourites, ${syncResponse.activities_synced} activities")
                
                // Mark synced items
                markItemsAsSynced(reviews, favourites, activities, userId)
                
                // Handle errors
                if (syncResponse.errors.isNotEmpty()) {
                    Log.w(TAG, "Sync completed with ${syncResponse.errors.size} errors")
                    syncResponse.errors.forEach { error ->
                        Log.w(TAG, "Sync error: ${error.type} - ${error.error}")
                    }
                }
            } else {
                Log.e(TAG, "Sync API call failed: ${response.code()} ${response.message()}")
                markItemsAsFailed(reviews, favourites, activities, userId)
                throw Exception("Sync failed: ${response.code()}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            markItemsAsFailed(reviews, favourites, activities, userId)
            throw e
        }
    }
    
    /**
     * Mark items as syncing.
     */
    private suspend fun markItemsAsSyncing(
        reviews: List<SyncReview>,
        favourites: List<SyncFavourite>,
        activities: List<SyncActivity>,
        userId: String
    ) {
        val reviewDao = database.reviewDao()
        val favoriteDao = database.favoriteTrailDao()
        val progressDao = database.trailProgressDao()
        
        reviews.forEach { review ->
            val entity = reviewDao.getAllReviews().find { 
                it.trailId == review.trail_id && it.userId == userId 
            }
            entity?.let { reviewDao.insertReview(it.copy(syncStatus = SyncStatus.SYNCING.name)) }
        }
        
        favourites.forEach { fav ->
            val entity = favoriteDao.getFavoritesByStatuses(
                listOf(SyncStatus.PENDING.name, SyncStatus.FAILED.name)
            ).find { it.trailId == fav.trail_id && it.userId == userId }
            entity?.let { favoriteDao.updateFavorite(it.markSyncing()) }
        }
        
        activities.forEach { activity ->
            val entity = progressDao.getProgressByStatuses(
                listOf(SyncStatus.PENDING.name, SyncStatus.FAILED.name)
            ).find { it.trailId == activity.trail_id && it.userId == userId && it.isCompleted }
            entity?.let { progressDao.updateProgress(it.markSyncing()) }
        }
    }
    
    /**
     * Mark items as synced.
     */
    private suspend fun markItemsAsSynced(
        reviews: List<SyncReview>,
        favourites: List<SyncFavourite>,
        activities: List<SyncActivity>,
        userId: String
    ) {
        val reviewDao = database.reviewDao()
        val favoriteDao = database.favoriteTrailDao()
        val progressDao = database.trailProgressDao()
        
        reviews.forEach { review ->
            val entity = reviewDao.getAllReviews().find { 
                it.trailId == review.trail_id && it.userId == userId 
            }
            entity?.let { 
                reviewDao.insertReview(it.copy(
                    syncStatus = SyncStatus.SYNCED.name,
                    lastSyncedAt = System.currentTimeMillis()
                ))
            }
        }
        
        favourites.forEach { fav ->
            val entity = favoriteDao.getFavoritesByStatuses(
                listOf(SyncStatus.SYNCING.name)
            ).find { it.trailId == fav.trail_id && it.userId == userId }
            entity?.let { favoriteDao.updateFavorite(it.markSynced()) }
        }
        
        activities.forEach { activity ->
            val entity = progressDao.getProgressByStatuses(
                listOf(SyncStatus.SYNCING.name)
            ).find { it.trailId == activity.trail_id && it.userId == userId && it.isCompleted }
            entity?.let { progressDao.updateProgress(it.markSynced()) }
        }
    }
    
    /**
     * Mark items as failed.
     */
    private suspend fun markItemsAsFailed(
        reviews: List<SyncReview>,
        favourites: List<SyncFavourite>,
        activities: List<SyncActivity>,
        userId: String
    ) {
        val reviewDao = database.reviewDao()
        val favoriteDao = database.favoriteTrailDao()
        val progressDao = database.trailProgressDao()
        
        reviews.forEach { review ->
            val entity = reviewDao.getAllReviews().find { 
                it.trailId == review.trail_id && it.userId == userId 
            }
            entity?.let { reviewDao.insertReview(it.copy(syncStatus = SyncStatus.FAILED.name)) }
        }
        
        favourites.forEach { fav ->
            val entity = favoriteDao.getFavoritesByStatuses(
                listOf(SyncStatus.SYNCING.name)
            ).find { it.trailId == fav.trail_id && it.userId == userId }
            entity?.let { favoriteDao.updateFavorite(it.markSyncFailed()) }
        }
        
        activities.forEach { activity ->
            val entity = progressDao.getProgressByStatuses(
                listOf(SyncStatus.SYNCING.name)
            ).find { it.trailId == activity.trail_id && it.userId == userId && it.isCompleted }
            entity?.let { progressDao.updateProgress(it.markSyncFailed()) }
        }
    }
}

