package com.trailguide.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for favorite trails with offline-first sync support.
 */
@Dao
interface FavoriteTrailDao {
    
    @Query("SELECT * FROM favorite_trails WHERE userId = :userId ORDER BY addedAt DESC")
    fun getFavoritesByUserId(userId: String): Flow<List<FavoriteTrailEntity>>
    
    @Query("SELECT * FROM favorite_trails WHERE id = :id")
    suspend fun getFavoriteById(id: String): FavoriteTrailEntity?
    
    @Query("SELECT * FROM favorite_trails WHERE userId = :userId AND trailId = :trailId LIMIT 1")
    suspend fun getFavoriteByUserAndTrail(userId: String, trailId: String): FavoriteTrailEntity?
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_trails WHERE userId = :userId AND trailId = :trailId)")
    suspend fun isFavorite(userId: String, trailId: String): Boolean
    
    @Query("SELECT * FROM favorite_trails WHERE syncStatus = :status")
    suspend fun getFavoritesByStatus(status: String): List<FavoriteTrailEntity>
    
    @Query("SELECT * FROM favorite_trails WHERE syncStatus IN (:statuses)")
    suspend fun getFavoritesByStatuses(statuses: List<String>): List<FavoriteTrailEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteTrailEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<FavoriteTrailEntity>)
    
    @Update
    suspend fun updateFavorite(favorite: FavoriteTrailEntity)
    
    @Query("DELETE FROM favorite_trails WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)
    
    @Query("DELETE FROM favorite_trails WHERE userId = :userId AND trailId = :trailId")
    suspend fun deleteFavoriteByUserAndTrail(userId: String, trailId: String)
    
    @Query("DELETE FROM favorite_trails WHERE userId = :userId")
    suspend fun deleteAllFavoritesForUser(userId: String)
    
    @Query("SELECT COUNT(*) FROM favorite_trails WHERE userId = :userId")
    suspend fun getFavoriteCount(userId: String): Int
}

