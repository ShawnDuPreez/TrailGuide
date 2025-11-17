package com.trailguide.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "navigation_sessions")
data class NavigationSessionEntity(
    @PrimaryKey
    val id: String,
    val trailId: String,
    val trailName: String,
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long? = null,
    val totalDistanceMeters: Double = 0.0,
    val distanceTraveledMeters: Double = 0.0,
    val durationMillis: Long = 0L,
    val pausedDurationMillis: Long = 0L,
    val averagePaceMinPerKm: Double? = null,
    val elevationGainMeters: Int? = null,
    val maxElevationMeters: Int? = null,
    val minElevationMeters: Int? = null,
    val currentState: String = "ACTIVE",
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncedAtMillis: Long? = null
)

