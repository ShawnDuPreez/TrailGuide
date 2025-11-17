package com.trailguide.android.data.repository

import com.trailguide.android.data.local.SyncStatus
import com.trailguide.android.data.local.TrailProgressDao
import com.trailguide.android.data.local.TrailProgressEntity
import com.trailguide.android.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing trail progress with offline-first sync.
 */
@Singleton
class TrailProgressRepository @Inject constructor(
    private val progressDao: TrailProgressDao,
    private val syncScheduler: SyncScheduler
) {
    
    /**
     * Get all progress for a user (reactive flow).
     */
    fun getProgressForUser(userId: String): Flow<List<TrailProgressEntity>> {
        return progressDao.getProgressByUserId(userId)
    }
    
    /**
     * Get progress for specific trail.
     */
    suspend fun getProgressForTrail(userId: String, trailId: String): TrailProgressEntity? {
        return progressDao.getProgressByUserAndTrail(userId, trailId)
    }
    
    /**
     * Get completed trails for user.
     */
    fun getCompletedTrails(userId: String): Flow<List<TrailProgressEntity>> {
        return progressDao.getCompletedTrailsByUserId(userId)
    }
    
    /**
     * Get in-progress trails for user.
     */
    fun getInProgressTrails(userId: String): Flow<List<TrailProgressEntity>> {
        return progressDao.getInProgressTrailsByUserId(userId)
    }
    
    /**
     * Start tracking progress for a trail.
     */
    suspend fun startProgress(userId: String, trailId: String) {
        val progress = TrailProgressEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            trailId = trailId,
            progressPercent = 0,
            distanceCoveredKm = 0.0,
            startedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        
        progressDao.insertProgress(progress)
        syncScheduler.scheduleOneTimeSync()
    }
    
    /**
     * Update trail progress (offline-first).
     */
    suspend fun updateProgress(
        userId: String,
        trailId: String,
        progressPercent: Int,
        distanceCoveredKm: Double,
        notes: String? = null
    ) {
        val existing = progressDao.getProgressByUserAndTrail(userId, trailId)
        
        if (existing != null) {
            val updated = existing.copy(
                progressPercent = progressPercent,
                distanceCoveredKm = distanceCoveredKm,
                notes = notes,
                lastUpdatedAt = System.currentTimeMillis(),
                completedAt = if (progressPercent >= 100) System.currentTimeMillis() else existing.completedAt,
                syncStatus = SyncStatus.PENDING.name
            )
            
            progressDao.updateProgress(updated)
            syncScheduler.scheduleOneTimeSync()
        }
    }
    
    /**
     * Mark trail as completed.
     */
    suspend fun completeTrail(userId: String, trailId: String, notes: String? = null) {
        val existing = progressDao.getProgressByUserAndTrail(userId, trailId)
        
        if (existing != null) {
            val completed = existing.copy(
                progressPercent = 100,
                completedAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis(),
                notes = notes,
                syncStatus = SyncStatus.PENDING.name
            )
            
            progressDao.updateProgress(completed)
            syncScheduler.scheduleOneTimeSync()
        }
    }
    
    /**
     * Delete progress for a trail.
     */
    suspend fun deleteProgress(userId: String, trailId: String) {
        val progress = progressDao.getProgressByUserAndTrail(userId, trailId)
        if (progress != null) {
            progressDao.deleteProgressById(progress.id)
            syncScheduler.scheduleOneTimeSync()
        }
    }
    
    /**
     * Get count of completed trails.
     */
    suspend fun getCompletedCount(userId: String): Int {
        return progressDao.getCompletedTrailCount(userId)
    }
}

