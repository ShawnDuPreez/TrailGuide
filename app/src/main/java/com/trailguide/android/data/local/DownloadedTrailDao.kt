package com.trailguide.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for downloaded trails.
 * Provides database operations for offline trail storage.
 */
@Dao
interface DownloadedTrailDao {
    
    /**
     * Get all downloaded trails.
     * Returns a Flow for reactive updates.
     */
    @Query("SELECT * FROM downloaded_trails ORDER BY downloadedAt DESC")
    fun getAllDownloadedTrails(): Flow<List<DownloadedTrailEntity>>
    
    /**
     * Get a specific downloaded trail by ID.
     */
    @Query("SELECT * FROM downloaded_trails WHERE id = :trailId")
    suspend fun getDownloadedTrail(trailId: String): DownloadedTrailEntity?
    
    /**
     * Check if a trail is downloaded.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_trails WHERE id = :trailId)")
    suspend fun isTrailDownloaded(trailId: String): Boolean
    
    /**
     * Get total storage used by downloads (in bytes).
     */
    @Query("SELECT SUM(sizeBytes) FROM downloaded_trails")
    suspend fun getTotalStorageUsed(): Long?
    
    /**
     * Get count of downloaded trails.
     */
    @Query("SELECT COUNT(*) FROM downloaded_trails")
    suspend fun getDownloadCount(): Int
    
    /**
     * Insert or replace a downloaded trail.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrail(trail: DownloadedTrailEntity)
    
    /**
     * Delete a specific trail.
     */
    @Delete
    suspend fun deleteTrail(trail: DownloadedTrailEntity)
    
    /**
     * Delete a trail by ID.
     */
    @Query("DELETE FROM downloaded_trails WHERE id = :trailId")
    suspend fun deleteTrailById(trailId: String)
    
    /**
     * Delete all downloaded trails.
     */
    @Query("DELETE FROM downloaded_trails")
    suspend fun deleteAllTrails()
}

