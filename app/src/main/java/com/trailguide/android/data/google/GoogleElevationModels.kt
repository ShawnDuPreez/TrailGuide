package com.trailguide.android.data.google

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Google Elevation API response models for hiking trail elevation profiles.
 */

@Serializable
data class ElevationResponse(
    val results: List<ElevationResult> = emptyList(),
    val status: String,
    @SerialName("error_message")
    val errorMessage: String? = null
)

@Serializable
data class ElevationResult(
    val elevation: Double, // meters above sea level
    val location: LatLng,
    val resolution: Double? = null // accuracy of elevation data
)

/**
 * Calculate elevation gain from a list of elevation results.
 */
fun List<ElevationResult>.calculateElevationGain(): Double {
    if (size < 2) return 0.0
    
    var totalGain = 0.0
    var totalLoss = 0.0
    
    for (i in 1 until size) {
        val diff = this[i].elevation - this[i - 1].elevation
        if (diff > 0) {
            totalGain += diff
        } else {
            totalLoss += kotlin.math.abs(diff)
        }
    }
    
    return totalGain
}

/**
 * Calculate elevation loss from a list of elevation results.
 */
fun List<ElevationResult>.calculateElevationLoss(): Double {
    if (size < 2) return 0.0
    
    var totalLoss = 0.0
    
    for (i in 1 until size) {
        val diff = this[i].elevation - this[i - 1].elevation
        if (diff < 0) {
            totalLoss += kotlin.math.abs(diff)
        }
    }
    
    return totalLoss
}

/**
 * Get highest elevation point.
 */
fun List<ElevationResult>.maxElevation(): Double? {
    return maxOfOrNull { it.elevation }
}

/**
 * Get lowest elevation point.
 */
fun List<ElevationResult>.minElevation(): Double? {
    return minOfOrNull { it.elevation }
}

