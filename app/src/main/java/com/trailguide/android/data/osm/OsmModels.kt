package com.trailguide.android.data.osm

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OSM Overpass API Response Models
 * Used for fetching real hiking trails from OpenStreetMap
 */

/**
 * Main response from Overpass API
 */
@Serializable
data class OverpassResponse(
    @SerialName("version") val version: Double? = null,
    @SerialName("generator") val generator: String? = null,
    @SerialName("elements") val elements: List<OsmElement> = emptyList()
)

/**
 * OSM Element (node, way, or relation)
 */
@Serializable
data class OsmElement(
    @SerialName("type") val type: String, // "node", "way", or "relation"
    @SerialName("id") val id: Long,
    @SerialName("lat") val lat: Double? = null, // For nodes
    @SerialName("lon") val lon: Double? = null, // For nodes
    @SerialName("nodes") val nodes: List<Long>? = null, // For ways
    @SerialName("geometry") val geometry: List<OsmGeometry>? = null, // Way geometry with coords
    @SerialName("tags") val tags: Map<String, String>? = null
)

/**
 * Geometry point (lat/lon pair)
 */
@Serializable
data class OsmGeometry(
    @SerialName("lat") val lat: Double,
    @SerialName("lon") val lon: Double
)

/**
 * Domain model for OSM hiking trail
 * Converted from Overpass API response
 */
data class OsmTrail(
    val id: Long,
    val name: String,
    val geometry: List<LatLng>, // Actual trail path
    val tags: Map<String, String>,
    val distance: Double, // Calculated in meters
    val difficulty: TrailDifficulty,
    val trailType: TrailType,
    val surface: String?,
    val description: String?
) {
    val startPoint: LatLng?
        get() = geometry.firstOrNull()
    
    val endPoint: LatLng?
        get() = geometry.lastOrNull()
    
    val midPoint: LatLng?
        get() = if (geometry.isNotEmpty()) geometry[geometry.size / 2] else null
}

/**
 * Trail difficulty based on OSM tags
 */
enum class TrailDifficulty {
    EASY,
    MODERATE,
    DIFFICULT,
    EXPERT,
    UNKNOWN;
    
    companion object {
        fun fromOsmTags(tags: Map<String, String>): TrailDifficulty {
            // Check various OSM difficulty tags
            val sac = tags["sac_scale"] // SAC (Swiss Alpine Club) hiking scale
            val trail = tags["trail_visibility"]
            val surface = tags["surface"]
            val incline = tags["incline"]
            
            return when {
                sac == "hiking" || sac == "mountain_hiking" -> EASY
                sac == "demanding_mountain_hiking" -> MODERATE
                sac == "alpine_hiking" || sac == "demanding_alpine_hiking" -> DIFFICULT
                sac == "difficult_alpine_hiking" -> EXPERT
                trail == "excellent" || trail == "good" -> EASY
                trail == "intermediate" -> MODERATE
                trail == "bad" || trail == "horrible" -> DIFFICULT
                surface == "paved" || surface == "asphalt" -> EASY
                surface == "gravel" || surface == "compacted" -> MODERATE
                surface == "dirt" || surface == "ground" -> MODERATE
                surface == "rock" || surface == "boulder" -> DIFFICULT
                incline?.contains("steep") == true -> DIFFICULT
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Trail type based on OSM highway tag
 */
enum class TrailType(val osmValue: String, val displayName: String) {
    FOOTWAY("footway", "Footway"),
    PATH("path", "Hiking Path"),
    TRACK("track", "Track"),
    BRIDLEWAY("bridleway", "Bridleway"),
    CYCLEWAY("cycleway", "Cycleway"),
    STEPS("steps", "Steps"),
    UNKNOWN("", "Unknown");
    
    companion object {
        fun fromOsmTag(highway: String?): TrailType {
            return values().find { it.osmValue == highway } ?: UNKNOWN
        }
    }
}

/**
 * Nominatim search result
 */
@Serializable
data class NominatimResult(
    @SerialName("osm_type") val osmType: String, // "relation", "way", "node"
    @SerialName("osm_id") val osmId: Long,
    @SerialName("display_name") val displayName: String,
    @SerialName("lat") val lat: String,
    @SerialName("lon") val lon: String,
    @SerialName("geojson") val geoJson: GeoJsonGeometry? = null,
    @SerialName("boundingbox") val boundingBox: List<String>? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("class") val osmClass: String? = null
)

/**
 * GeoJSON geometry from Nominatim
 */
@Serializable
data class GeoJsonGeometry(
    @SerialName("type") val type: String, // "Polygon", "MultiPolygon", "Point"
    @SerialName("coordinates") val coordinates: kotlinx.serialization.json.JsonElement
)

/**
 * Domain model for nature reserve/place boundary
 */
data class OsmBoundary(
    val osmId: Long,
    val osmType: String,
    val name: String,
    val centerLat: Double,
    val centerLon: Double,
    val polygon: List<LatLng>, // Outer boundary
    val boundingBox: BoundingBox? = null
) {
    /**
     * Convert OSM relation ID to Overpass area ID
     * Formula: areaId = relationId + 3600000000
     */
    fun toAreaId(): Long = when (osmType) {
        "relation" -> osmId + 3600000000
        "way" -> osmId + 2400000000
        else -> osmId
    }
    
    /**
     * Get center point
     */
    val center: LatLng
        get() = LatLng(centerLat, centerLon)
}

/**
 * Bounding box for a boundary
 */
data class BoundingBox(
    val south: Double,
    val north: Double,
    val west: Double,
    val east: Double
)

/**
 * Extension functions for converting OSM elements to domain models
 */

/**
 * Convert OSM element to OsmTrail
 */
fun OsmElement.toOsmTrail(nodeMap: Map<Long, LatLng> = emptyMap()): OsmTrail? {
    if (type != "way") return null
    
    val tags = this.tags ?: emptyMap()
    val geometry = this.geometry?.map { LatLng(it.lat, it.lon) } ?: emptyList()
    
    if (geometry.isEmpty()) return null
    
    val name = tags["name"] 
        ?: tags["ref"] 
        ?: "Trail ${id}"
    
    val distance = calculateDistance(geometry)
    val difficulty = TrailDifficulty.fromOsmTags(tags)
    val trailType = TrailType.fromOsmTag(tags["highway"])
    val surface = tags["surface"]
    val description = tags["description"]
    
    return OsmTrail(
        id = id,
        name = name,
        geometry = geometry,
        tags = tags,
        distance = distance,
        difficulty = difficulty,
        trailType = trailType,
        surface = surface,
        description = description
    )
}

/**
 * Calculate trail distance in meters using Haversine formula
 */
private fun calculateDistance(points: List<LatLng>): Double {
    if (points.size < 2) return 0.0
    
    var totalDistance = 0.0
    for (i in 0 until points.size - 1) {
        totalDistance += haversineDistance(points[i], points[i + 1])
    }
    return totalDistance
}

/**
 * Calculate distance between two points using Haversine formula
 */
private fun haversineDistance(point1: LatLng, point2: LatLng): Double {
    val R = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(point2.latitude - point1.latitude)
    val dLon = Math.toRadians(point2.longitude - point1.longitude)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return R * c
}
