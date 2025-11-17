package com.trailguide.android.data.repository

import com.trailguide.android.data.local.FavoriteTrailDao
import com.trailguide.android.data.local.FavoriteTrailEntity
import com.trailguide.android.data.local.SyncStatus
import com.trailguide.android.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing favorite trails with offline-first sync.
 */
@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteTrailDao,
    private val syncScheduler: SyncScheduler
) {
    
    /**
     * Get all favorites for a user (reactive flow).
     */
    fun getFavorites(userId: String): Flow<List<FavoriteTrailEntity>> {
        return favoriteDao.getFavoritesByUserId(userId)
    }
    
    /**
     * Check if a trail is favorited by user.
     */
    suspend fun isFavorite(userId: String, trailId: String): Boolean {
        return favoriteDao.isFavorite(userId, trailId)
    }
    
    /**
     * Add trail to favorites (offline-first).
     */
    suspend fun addFavorite(
        userId: String,
        trailId: String,
        trailName: String,
        trailImageUrl: String?
    ) {
        val favorite = FavoriteTrailEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            trailId = trailId,
            trailName = trailName,
            trailImageUrl = trailImageUrl,
            addedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        
        favoriteDao.insertFavorite(favorite)
        
        // Schedule sync
        syncScheduler.scheduleOneTimeSync()
    }
    
    /**
     * Remove trail from favorites (offline-first).
     */
    suspend fun removeFavorite(userId: String, trailId: String) {
        favoriteDao.deleteFavoriteByUserAndTrail(userId, trailId)
        
        // Schedule sync
        syncScheduler.scheduleOneTimeSync()
    }
    
    /**
     * Toggle favorite status.
     */
    suspend fun toggleFavorite(
        userId: String,
        trailId: String,
        trailName: String,
        trailImageUrl: String?
    ) {
        if (isFavorite(userId, trailId)) {
            removeFavorite(userId, trailId)
        } else {
            addFavorite(userId, trailId, trailName, trailImageUrl)
        }
    }
    
    /**
     * Get favorites count for user.
     */
    suspend fun getFavoritesCount(userId: String): Int {
        return favoriteDao.getFavoriteCount(userId)
    }
}

