package com.trailguide.android.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "navigation_waypoints",
    indices = [Index(value = ["sessionId"])]
)
data class NavigationWaypointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float? = null,
    val speedMetersPerSecond: Float? = null,
    val bearingDegrees: Float? = null
)

