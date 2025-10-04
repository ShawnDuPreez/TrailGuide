package com.trailguide.android.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Trail data model representing a hiking trail.
 * Parcelize annotation enables easy passing between activities/fragments.
 */
@Parcelize
data class Trail(
    val id: String,
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val elevationM: Int,
    val difficulty: Difficulty,
    val rating: Double,
    val imageUrl: String?,
    val tags: List<String> = emptyList(),
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val description: String? = null,
    val segments: List<TrailSegment> = emptyList()
) : Parcelable

/**
 * Trail difficulty levels.
 */
enum class Difficulty(val displayName: String, val order: Int) {
    EASY("Easy", 1),
    MODERATE("Moderate", 2),
    HARD("Hard", 3);

    companion object {
        fun fromString(value: String?): Difficulty {
            return when (value?.lowercase()) {
                "easy" -> EASY
                "moderate" -> MODERATE
                "hard" -> HARD
                else -> MODERATE
            }
        }
    }
}

/**
 * Represents a segment of a trail with specific characteristics.
 */
@Parcelize
data class TrailSegment(
    val name: String,
    val type: String, // e.g., "Steep", "Exposed", "Family"
    val distance: Double? = null,
    val elevation: Int? = null
) : Parcelable

