package com.trailguide.android.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trailguide.android.data.local.*
import com.trailguide.android.data.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            
            // Sync favorites
            syncFavorites(userId)
            
            // Sync trail progress
            syncTrailProgress(userId)
            
            // Sync reviews
            syncReviews(userId)
            
            // Sync collections
            syncCollections(userId)
            
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
     * Sync favorite trails with backend.
     */
    private suspend fun syncFavorites(userId: String) {
        try {
            val favoriteDao = database.favoriteTrailDao()
            
            // Get all favorites that need syncing
            val pendingFavorites = favoriteDao.getFavoritesByStatuses(
                listOf(SyncStatus.PENDING.name, SyncStatus.FAILED.name)
            )
            
            Log.d(TAG, "Syncing ${pendingFavorites.size} favorites")
            
            for (favorite in pendingFavorites) {
                try {
                    // Mark as syncing
                    favoriteDao.updateFavorite(favorite.markSyncing())
                    
                    // TODO: Sync with Supabase API
                    // For now, just mark as synced
                    
                    // Mark as synced
                    favoriteDao.updateFavorite(favorite.markSynced())
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync favorite ${favorite.id}", e)
                    favoriteDao.updateFavorite(favorite.markSyncFailed())
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing favorites", e)
            throw e
        }
    }
    
    /**
     * Sync trail progress with backend.
     */
    private suspend fun syncTrailProgress(userId: String) {
        try {
            val progressDao = database.trailProgressDao()
            
            // Get all progress that needs syncing
            val pendingProgress = progressDao.getProgressByStatuses(
                listOf(SyncStatus.PENDING.name, SyncStatus.FAILED.name)
            )
            
            Log.d(TAG, "Syncing ${pendingProgress.size} progress items")
            
            for (progress in pendingProgress) {
                try {
                    // Mark as syncing
                    progressDao.updateProgress(progress.markSyncing())
                    
                    // TODO: Sync with Supabase API
                    // For now, just mark as synced
                    
                    // Mark as synced
                    progressDao.updateProgress(progress.markSynced())
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync progress ${progress.id}", e)
                    progressDao.updateProgress(progress.markSyncFailed())
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing trail progress", e)
            throw e
        }
    }
    
    /**
     * Sync reviews with backend.
     */
    private suspend fun syncReviews(userId: String) {
        try {
            val reviewDao = database.reviewDao()
            
            // Get all reviews for this user
            val allReviews = reviewDao.getAllReviews()
            val userReviews = allReviews.filter { 
                it.userId == userId && 
                (it.syncStatus == SyncStatus.PENDING.name || it.syncStatus == SyncStatus.FAILED.name)
            }
            
            Log.d(TAG, "Syncing ${userReviews.size} reviews")
            
            for (review in userReviews) {
                try {
                    // Mark as syncing
                    reviewDao.insertReview(review.copy(syncStatus = SyncStatus.SYNCING.name))
                    
                    // TODO: Sync with Supabase API
                    // For now, just mark as synced
                    
                    // Mark as synced
                    reviewDao.insertReview(review.copy(
                        syncStatus = SyncStatus.SYNCED.name,
                        lastSyncedAt = System.currentTimeMillis()
                    ))
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync review ${review.id}", e)
                    reviewDao.insertReview(review.copy(syncStatus = SyncStatus.FAILED.name))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing reviews", e)
            throw e
        }
    }
    
    /**
     * Sync collections with backend.
     */
    private suspend fun syncCollections(userId: String) {
        try {
            val collectionDao = database.collectionDao()
            
            // Get all collections
            val allCollections = collectionDao.getAllCollections()
            val pendingCollections = allCollections.filter {
                it.syncStatus == SyncStatus.PENDING.name || it.syncStatus == SyncStatus.FAILED.name
            }
            
            Log.d(TAG, "Syncing ${pendingCollections.size} collections")
            
            for (collection in pendingCollections) {
                try {
                    // Mark as syncing
                    collectionDao.insertCollection(collection.copy(syncStatus = SyncStatus.SYNCING.name))
                    
                    // TODO: Sync with Supabase API
                    // For now, just mark as synced
                    
                    // Mark as synced
                    collectionDao.insertCollection(collection.copy(
                        syncStatus = SyncStatus.SYNCED.name,
                        lastSyncedAt = System.currentTimeMillis()
                    ))
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync collection ${collection.id}", e)
                    collectionDao.insertCollection(collection.copy(syncStatus = SyncStatus.FAILED.name))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing collections", e)
            throw e
        }
    }
}

