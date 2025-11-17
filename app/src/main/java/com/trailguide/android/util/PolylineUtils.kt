package com.trailguide.android.util

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlin.math.max

/**
 * Utility helpers for working with polylines / navigation routes.
 */
object PolylineUtils {

    private const val SAMPLE_POINTS_PER_SEGMENT = 8
    private const val MIN_TOTAL_DISTANCE_METERS = 1.0
    private const val MILLIS_IN_MINUTE = 60_000L

    data class PolylineProgress(
        val snappedPoint: LatLng,
        val distanceFromStartMeters: Double,
        val distanceToEndMeters: Double,
        val progressPercent: Float,
        val nearestSegmentIndex: Int
    )

    /**
     * Calculates progress metrics for the given [userLocation] along the provided [polyline].
     * Returns null if the polyline is empty or invalid.
     */
    fun computeProgress(userLocation: LatLng, polyline: List<LatLng>): PolylineProgress? {
        if (polyline.size < 2) return null

        val totalDistance = calculateTotalDistance(polyline)
        if (totalDistance < MIN_TOTAL_DISTANCE_METERS) return null

        var accumulatedDistance = 0.0
        var closestDistance = Double.MAX_VALUE
        var bestSnappedPoint = polyline.first()
        var bestDistanceFromStart = 0.0
        var bestSegmentIndex = 0

        polyline.windowed(size = 2, step = 1).forEachIndexed { index, segment ->
            val start = segment[0]
            val end = segment[1]
            val segmentDistance = SphericalUtil.computeDistanceBetween(start, end)

            // Sample points along the segment to approximate the closest point
            for (i in 0..SAMPLE_POINTS_PER_SEGMENT) {
                val fraction = i / SAMPLE_POINTS_PER_SEGMENT.toDouble()
                val samplePoint = SphericalUtil.interpolate(start, end, fraction)
                val distanceToSample = SphericalUtil.computeDistanceBetween(userLocation, samplePoint)

                if (distanceToSample < closestDistance) {
                    closestDistance = distanceToSample
                    bestSnappedPoint = samplePoint
                    bestDistanceFromStart = accumulatedDistance + (segmentDistance * fraction)
                    bestSegmentIndex = index
                }
            }

            accumulatedDistance += segmentDistance
        }

        val distanceFromStart = bestDistanceFromStart
        val distanceToEnd = max(0.0, totalDistance - distanceFromStart)
        val progressPercent = (distanceFromStart / totalDistance).coerceIn(0.0, 1.0).toFloat()

        return PolylineProgress(
            snappedPoint = bestSnappedPoint,
            distanceFromStartMeters = distanceFromStart,
            distanceToEndMeters = distanceToEnd,
            progressPercent = progressPercent * 100f,
            nearestSegmentIndex = bestSegmentIndex
        )
    }

    /**
     * Calculates the total length of the provided [polyline].
     */
    fun calculateTotalDistance(polyline: List<LatLng>): Double {
        if (polyline.size < 2) return 0.0
        var distance = 0.0
        polyline.windowed(size = 2, step = 1).forEach { segment ->
            distance += SphericalUtil.computeDistanceBetween(segment[0], segment[1])
        }
        return distance
    }

    /**
     * Calculates the pace (minutes per kilometer) given elapsed time and distance.
     */
    fun calculatePaceMinPerKm(elapsedMillis: Long, distanceMeters: Double): Double? {
        if (elapsedMillis <= 0 || distanceMeters <= 0) return null
        val minutes = elapsedMillis / MILLIS_IN_MINUTE.toDouble()
        val kilometers = distanceMeters / 1000.0
        if (kilometers <= 0) return null
        return minutes / kilometers
    }

    /**
     * Estimates ETA in millis based on remaining distance and pace.
     * Returns null if data is insufficient.
     */
    fun calculateEtaMillis(
        currentTimeMillis: Long,
        distanceRemainingMeters: Double,
        paceMinPerKm: Double?
    ): Long? {
        if (paceMinPerKm == null || distanceRemainingMeters <= 0) return null
        val minutesRemaining = paceMinPerKm * (distanceRemainingMeters / 1000.0)
        val millisRemaining = (minutesRemaining * MILLIS_IN_MINUTE).toLong()
        return currentTimeMillis + millisRemaining
    }
}

