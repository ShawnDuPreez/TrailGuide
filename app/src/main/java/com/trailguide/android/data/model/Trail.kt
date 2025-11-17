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
    val city: String? = null,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double? = null, // Auto-fetched from Directions API
    val elevationM: Int? = null, // Auto-fetched from Elevation API
    val difficulty: Difficulty? = null, // Optional for now
    val rating: Double? = null, // Auto-fetched from Places API
    val reviewCount: Int? = null, // Auto-fetched from Places API
    val imageUrl: String? = null, // Auto-fetched from Places API
    val tags: List<String> = emptyList(),
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val description: String? = null, // Auto-fetched from Places API
    val duration: String? = null, // Auto-calculated from distance/elevation
    val segments: List<TrailSegment> = emptyList(), // Auto-generated from Directions steps
    val routeCoordinates: List<RoutePoint> = emptyList(), // Auto-fetched from Directions polyline
    val formattedAddress: String? = null, // From Places API
    val website: String? = null, // From Places API
    val phoneNumber: String? = null // From Places API
) : Parcelable {
    // Computed properties
    val distance: Double? get() = distanceKm
    val elevationGain: Int? get() = elevationM
}

/**
 * GPS coordinate point for trail route visualization
 */
@Parcelize
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null
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
 * Auto-generated from Google Directions API steps.
 */
@Parcelize
data class TrailSegment(
    val name: String, // e.g., "Trailhead → River Crossing"
    val description: String? = null, // Instructions from Directions API
    val type: String? = null, // e.g., "Steep", "Exposed", "Family" (can be inferred)
    val distance: Double? = null, // Distance in meters
    val duration: Int? = null, // Duration in seconds
    val elevation: Int? = null, // Elevation gain in meters
    val startPoint: RoutePoint? = null,
    val endPoint: RoutePoint? = null
) : Parcelable

