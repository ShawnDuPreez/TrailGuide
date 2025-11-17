package com.trailguide.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for trail progress tracking.
 * Tracks completion percentage and other progress metrics.
 */
@Entity(tableName = "trail_progress")
data class TrailProgressEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val trailId: String,
    val trailName: String = "Unknown Trail",
    val progressPercent: Int,
    val distanceCoveredKm: Double,
    val startedAt: Long,
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isCompleted: Boolean = false,
    val notes: String? = null,
    val syncStatus: String = SyncStatus.PENDING.name,
    val lastSyncedAt: Long = 0L
) {
    // Computed properties for UI
    val progressPercentage: Float
        get() = progressPercent / 100f
    
    val distanceCovered: Float
        get() = (distanceCoveredKm * 1000).toFloat()
}

/**
 * Update sync status of progress
 */
fun TrailProgressEntity.markSynced(): TrailProgressEntity {
    return copy(
        syncStatus = SyncStatus.SYNCED.name,
        lastSyncedAt = System.currentTimeMillis()
    )
}

/**
 * Mark sync as failed
 */
fun TrailProgressEntity.markSyncFailed(): TrailProgressEntity {
    return copy(syncStatus = SyncStatus.FAILED.name)
}

/**
 * Mark as syncing
 */
fun TrailProgressEntity.markSyncing(): TrailProgressEntity {
    return copy(syncStatus = SyncStatus.SYNCING.name)
}

/**
 * Check if progress is completed (extension function for backward compatibility)
 */
fun TrailProgressEntity.isCompletedExt(): Boolean {
    return isCompleted || completedAt != null || progressPercent >= 100
}

