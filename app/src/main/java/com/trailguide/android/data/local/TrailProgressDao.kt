package com.trailguide.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for trail progress tracking with offline-first sync support.
 */
@Dao
interface TrailProgressDao {
    
    @Query("SELECT * FROM trail_progress WHERE userId = :userId ORDER BY lastUpdatedAt DESC")
    fun getProgressByUserId(userId: String): Flow<List<TrailProgressEntity>>
    
    @Query("SELECT * FROM trail_progress WHERE id = :id")
    suspend fun getProgressById(id: String): TrailProgressEntity?
    
    @Query("SELECT * FROM trail_progress WHERE userId = :userId AND trailId = :trailId LIMIT 1")
    suspend fun getProgressByUserAndTrail(userId: String, trailId: String): TrailProgressEntity?
    
    @Query("SELECT * FROM trail_progress WHERE syncStatus = :status")
    suspend fun getProgressByStatus(status: String): List<TrailProgressEntity>
    
    @Query("SELECT * FROM trail_progress WHERE syncStatus IN (:statuses)")
    suspend fun getProgressByStatuses(statuses: List<String>): List<TrailProgressEntity>
    
    @Query("SELECT * FROM trail_progress WHERE userId = :userId AND completedAt IS NOT NULL ORDER BY completedAt DESC")
    fun getCompletedTrailsByUserId(userId: String): Flow<List<TrailProgressEntity>>
    
    @Query("SELECT * FROM trail_progress WHERE userId = :userId AND completedAt IS NULL ORDER BY lastUpdatedAt DESC")
    fun getInProgressTrailsByUserId(userId: String): Flow<List<TrailProgressEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: TrailProgressEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<TrailProgressEntity>)
    
    @Update
    suspend fun updateProgress(progress: TrailProgressEntity)
    
    @Query("DELETE FROM trail_progress WHERE id = :id")
    suspend fun deleteProgressById(id: String)
    
    @Query("DELETE FROM trail_progress WHERE userId = :userId")
    suspend fun deleteAllProgressForUser(userId: String)
    
    @Query("SELECT COUNT(*) FROM trail_progress WHERE userId = :userId AND completedAt IS NOT NULL")
    suspend fun getCompletedTrailCount(userId: String): Int
}

