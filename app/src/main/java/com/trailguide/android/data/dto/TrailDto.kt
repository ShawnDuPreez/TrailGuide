package com.trailguide.android.data.dto

import com.google.gson.annotations.SerializedName
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.model.TrailSegment

/**
 * Data Transfer Object for Trail API responses.
 * Maps JSON fields from Supabase to Kotlin properties.
 */
data class TrailDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("city") val city: String,
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lon") val longitude: Double,
    @SerializedName("distance_km") val distanceKm: Double,
    @SerializedName("elevation_m") val elevationM: Int,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("rating") val rating: Double,
    @SerializedName("image") val imageUrl: String?,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("downloaded") val downloaded: Boolean? = null
)

/**
 * Extension function to convert DTO to domain model.
 */
fun TrailDto.toDomainModel(): Trail {
    return Trail(
        id = id,
        name = name,
        city = city,
        latitude = latitude,
        longitude = longitude,
        distanceKm = distanceKm,
        elevationM = elevationM,
        difficulty = Difficulty.fromString(difficulty),
        rating = rating,
        imageUrl = imageUrl,
        tags = tags ?: emptyList(),
        isDownloaded = downloaded ?: false,
        isFavorite = false,
        description = description,
        segments = generateDefaultSegments(name) // Generate mock segments for now
    )
}

/**
 * Generate default trail segments for demonstration.
 * In production, this would come from the API.
 */
private fun generateDefaultSegments(trailName: String): List<TrailSegment> {
    return listOf(
        TrailSegment(
            name = "Trailhead → River Crossing",
            type = "Family",
            distance = 2000.0,
            elevation = 50
        ),
        TrailSegment(
            name = "River Crossing → Ridge",
            type = "Steep",
            distance = 3500.0,
            elevation = 200
        ),
        TrailSegment(
            name = "Ridge → Summit",
            type = "Exposed",
            distance = 2900.0,
            elevation = 170
        )
    )
}

/**
 * API request for creating/updating trails.
 */
data class CreateTrailRequest(
    val id: String? = null,
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val elevationM: Int,
    val difficulty: String,
    val rating: Double,
    val imageUrl: String?,
    val tags: List<String>? = null,
    val description: String? = null
)

/**
 * Extension function to convert Trail model to CreateTrailRequest.
 */
fun Trail.toCreateRequest(): CreateTrailRequest {
    return CreateTrailRequest(
        id = id,
        name = name,
        city = city ?: "Unknown",
        latitude = latitude,
        longitude = longitude,
        distanceKm = distanceKm ?: 0.0,
        elevationM = elevationM ?: 0,
        difficulty = difficulty?.name?.lowercase() ?: "moderate",
        rating = rating ?: 0.0,
        imageUrl = imageUrl,
        tags = tags,
        description = description
    )
}

/**
 * Generic API response wrapper.
 */
data class ApiResponse<T>(
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: String?,
    @SerializedName("message") val message: String?
)

