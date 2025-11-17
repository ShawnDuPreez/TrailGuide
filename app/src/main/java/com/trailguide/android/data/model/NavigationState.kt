package com.trailguide.android.data.model

/**
 * Represents the live statistics for a navigation session.
 */
data class NavigationStats(
    val trailId: String,
    val trailName: String,
    val totalDistanceMeters: Double,
    val distanceTraveledMeters: Double,
    val distanceRemainingMeters: Double,
    val progressPercent: Float,
    val currentPaceMinPerKm: Double?,
    val averagePaceMinPerKm: Double?,
    val elapsedMillis: Long,
    val etaMillis: Long?,
    val elevationGainMeters: Int,
    val currentElevationMeters: Int?,
    val gpsAccuracyMeters: Float?,
    val lastKnownLatitude: Double? = null,
    val lastKnownLongitude: Double? = null,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents the current state of the navigation engine.
 */
sealed class NavigationState {
    object Idle : NavigationState()

    data class Preparing(
        val trailId: String,
        val trailName: String
    ) : NavigationState()

    data class Active(
        val sessionId: String,
        val stats: NavigationStats,
        val isPaused: Boolean = false
    ) : NavigationState()

    data class Completed(
        val sessionId: String,
        val stats: NavigationStats
    ) : NavigationState()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : NavigationState()
}

