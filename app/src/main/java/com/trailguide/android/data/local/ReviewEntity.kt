package com.trailguide.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trailguide.android.data.model.Review

/**
 * Room entity for reviews
 */
@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey
    val id: String,
    val trailId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val rating: Double,
    val comment: String,
    val photos: List<String>,
    val createdAt: Long,
    val likes: Int,
    val syncStatus: String = SyncStatus.PENDING.name,
    val lastSyncedAt: Long = 0L
)

fun ReviewEntity.toDomainModel(): Review {
    return Review(
        id = id,
        trailId = trailId,
        userId = userId,
        userName = userName,
        userAvatar = userAvatar,
        rating = rating,
        comment = comment,
        photos = photos,
        createdAt = createdAt,
        likes = likes
    )
}

fun Review.toEntity(): ReviewEntity {
    return ReviewEntity(
        id = id,
        trailId = trailId,
        userId = userId,
        userName = userName,
        userAvatar = userAvatar,
        rating = rating,
        comment = comment,
        photos = photos,
        createdAt = createdAt,
        likes = likes
    )
}

