package com.trailguide.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trailguide.android.data.model.TrailCollection

/**
 * Room entity for trail collections
 */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val trailIds: List<String>,
    val createdAt: Long,
    val coverImage: String?,
    val syncStatus: String = SyncStatus.PENDING.name,
    val lastSyncedAt: Long = 0L
)

fun CollectionEntity.toDomainModel(): TrailCollection {
    return TrailCollection(
        id = id,
        name = name,
        description = description,
        trailIds = trailIds,
        createdAt = createdAt,
        coverImage = coverImage
    )
}

fun TrailCollection.toEntity(): CollectionEntity {
    return CollectionEntity(
        id = id,
        name = name,
        description = description,
        trailIds = trailIds,
        createdAt = createdAt,
        coverImage = coverImage
    )
}

