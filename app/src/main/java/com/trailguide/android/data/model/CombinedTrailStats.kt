package com.trailguide.android.data.model

import com.trailguide.android.data.osm.OsmTrail

/**
 * Combined statistics for all OSM trails in a boundary
 * Note: Not serializable because OsmTrail contains LatLng which is not serializable
 */
data class CombinedTrailStats(
    val totalDistance: Double, // in meters
    val totalElevation: Double, // in meters
    val trailCount: Int,
    val osmTrails: List<OsmTrail>
)