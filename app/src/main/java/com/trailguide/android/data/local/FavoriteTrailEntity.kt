package com.trailguide.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for favorite trails.
 * Supports offline-first with sync status.
 */
@Entity(tableName = "favorite_trails")
data class FavoriteTrailEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val trailId: String,
    val trailName: String,
    val trailImageUrl: String?,
    val location: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.PENDING.name,
    val lastSyncedAt: Long = 0L
) {
    // Computed property for UI
    val favoritedAt: Long
        get() = addedAt
}

/**
 * Update sync status of favorite
 */
fun FavoriteTrailEntity.markSynced(): FavoriteTrailEntity {
    return copy(
        syncStatus = SyncStatus.SYNCED.name,
        lastSyncedAt = System.currentTimeMillis()
    )
}

/**
 * Mark sync as failed
 */
fun FavoriteTrailEntity.markSyncFailed(): FavoriteTrailEntity {
    return copy(syncStatus = SyncStatus.FAILED.name)
}

/**
 * Mark as syncing
 */
fun FavoriteTrailEntity.markSyncing(): FavoriteTrailEntity {
    return copy(syncStatus = SyncStatus.SYNCING.name)
}

