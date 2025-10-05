package com.trailguide.android.data.osm

import com.google.android.gms.maps.model.LatLng

/**
 * Data classes for OpenStreetMap (OSM) data structures.
 * Used for parsing Overpass API responses.
 */

/**
 * Root response from Overpass API
 */
data class OverpassResponse(
    val version: Double,
    val generator: String,
    val osm3s: Osm3s,
    val elements: List<OsmElement>
)

/**
 * OSM3S metadata
 */
data class Osm3s(
    val timestamp_osm_base: String,
    val copyright: String
)

/**
 * Base OSM element (node, way, or relation)
 */
sealed class OsmElement {
    abstract val type: String
    abstract val id: Long
    abstract val tags: Map<String, String>?
}

/**
 * OSM Node - represents a point with latitude and longitude
 */
data class OsmNode(
    override val type: String,
    override val id: Long,
    val lat: Double,
    val lon: Double,
    override val tags: Map<String, String>? = null
) : OsmElement()

/**
 * OSM Way - represents a path or area defined by a list of node references
 */
data class OsmWay(
    override val type: String,
    override val id: Long,
    val nodes: List<Long>,
    val geometry: List<OsmNode>? = null,
    override val tags: Map<String, String>? = null
) : OsmElement()

/**
 * OSM Relation - represents a collection of ways, nodes, or other relations
 */
data class OsmRelation(
    override val type: String,
    override val id: Long,
    val members: List<OsmMember>,
    override val tags: Map<String, String>? = null
) : OsmElement()

/**
 * Member of an OSM relation
 */
data class OsmMember(
    val type: String,
    val ref: Long,
    val role: String,
    val geometry: List<OsmNode>? = null
)

/**
 * Processed hiking trail data ready for display on map
 */
data class HikingTrail(
    val id: String,
    val name: String?,
    val coordinates: List<LatLng>,
    val difficulty: TrailDifficulty?,
    val surface: String?,
    val description: String?
)

/**
 * Trail difficulty levels based on OSM tags
 */
enum class TrailDifficulty {
    EASY,
    MODERATE,
    HARD,
    UNKNOWN
}

/**
 * Extension function to convert OsmNode to LatLng
 */
fun OsmNode.toLatLng(): LatLng = LatLng(lat, lon)

/**
 * Extension function to get trail name from OSM tags
 */
fun Map<String, String>?.getTrailName(): String? {
    return this?.let { tags ->
        tags["name"] ?: tags["ref"] ?: tags["int_name"] ?: tags["loc_name"]
    }
}

/**
 * Extension function to determine trail difficulty from OSM tags
 */
fun Map<String, String>?.getTrailDifficulty(): TrailDifficulty {
    return this?.let { tags ->
        when {
            tags["sac_scale"]?.contains("hiking", ignoreCase = true) == true -> TrailDifficulty.EASY
            tags["sac_scale"]?.contains("mountain_hiking", ignoreCase = true) == true -> TrailDifficulty.MODERATE
            tags["sac_scale"]?.contains("alpine_hiking", ignoreCase = true) == true -> TrailDifficulty.HARD
            tags["difficulty"]?.contains("easy", ignoreCase = true) == true -> TrailDifficulty.EASY
            tags["difficulty"]?.contains("moderate", ignoreCase = true) == true -> TrailDifficulty.MODERATE
            tags["difficulty"]?.contains("hard", ignoreCase = true) == true -> TrailDifficulty.HARD
            else -> TrailDifficulty.UNKNOWN
        }
    } ?: TrailDifficulty.UNKNOWN
}

/**
 * Extension function to get trail surface from OSM tags
 */
fun Map<String, String>?.getTrailSurface(): String? {
    return this?.get("surface") ?: this?.get("tracktype")
}

/**
 * Extension function to get trail description from OSM tags
 */
fun Map<String, String>?.getTrailDescription(): String? {
    return this?.get("description") ?: this?.get("note")
}
