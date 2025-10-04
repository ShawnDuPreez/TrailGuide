package com.trailguide.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.data.model.RoutePoint
import com.trailguide.android.data.model.Trail

/**
 * Room entity for downloaded trails.
 * Stores trail data locally for offline access.
 */
@Entity(tableName = "downloaded_trails")
data class DownloadedTrailEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val elevationM: Int,
    val difficulty: String,
    val rating: Double,
    val imageUrl: String?,
    val tags: List<String>,
    val description: String?,
    val routeCoordinates: List<RoutePoint>,
    val downloadedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0
)

/**
 * Convert entity to domain model
 */
fun DownloadedTrailEntity.toDomainModel(): Trail {
    return Trail(
        id = id,
        name = name,
        city = city,
        latitude = latitude,
        longitude = longitude,
        distanceKm = distanceKm,
        elevationM = elevationM,
        difficulty = Difficulty.valueOf(difficulty.uppercase()),
        rating = rating,
        imageUrl = imageUrl,
        tags = tags,
        description = description,
        routeCoordinates = routeCoordinates,
        isDownloaded = true
    )
}

/**
 * Convert domain model to entity
 */
fun Trail.toEntity(): DownloadedTrailEntity {
    // Calculate approximate size (in bytes)
    val estimatedSize = (routeCoordinates.size * 100) + (description?.length ?: 0) * 2 + 5000
    
    return DownloadedTrailEntity(
        id = id,
        name = name,
        city = city,
        latitude = latitude,
        longitude = longitude,
        distanceKm = distanceKm,
        elevationM = elevationM,
        difficulty = difficulty.name,
        rating = rating,
        imageUrl = imageUrl,
        tags = tags,
        description = description,
        routeCoordinates = routeCoordinates,
        sizeBytes = estimatedSize.toLong()
    )
}

