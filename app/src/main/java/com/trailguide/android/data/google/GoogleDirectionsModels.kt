package com.trailguide.android.data.google

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Google Directions API response models for route navigation.
 */

@Serializable
data class DirectionsResponse(
    val routes: List<DirectionRoute> = emptyList(),
    val status: String,
    @SerialName("error_message")
    val errorMessage: String? = null
)

@Serializable
data class DirectionRoute(
    val summary: String,
    val legs: List<RouteLeg>,
    @SerialName("overview_polyline")
    val overviewPolyline: Polyline,
    val bounds: LatLngBounds,
    @SerialName("waypoint_order")
    val waypointOrder: List<Int>? = null
)

@Serializable
data class RouteLeg(
    val distance: Distance,
    val duration: Duration,
    @SerialName("start_location")
    val startLocation: LatLng,
    @SerialName("end_location")
    val endLocation: LatLng,
    @SerialName("start_address")
    val startAddress: String,
    @SerialName("end_address")
    val endAddress: String,
    val steps: List<RouteStep>
)

@Serializable
data class RouteStep(
    val distance: Distance,
    val duration: Duration,
    @SerialName("start_location")
    val startLocation: LatLng,
    @SerialName("end_location")
    val endLocation: LatLng,
    @SerialName("html_instructions")
    val htmlInstructions: String,
    val polyline: Polyline,
    @SerialName("travel_mode")
    val travelMode: String,
    val maneuver: String? = null
)

@Serializable
data class Distance(
    val text: String,
    val value: Int // in meters
)

@Serializable
data class Duration(
    val text: String,
    val value: Int // in seconds
)

@Serializable
data class LatLng(
    val lat: Double,
    val lng: Double
)

@Serializable
data class Polyline(
    val points: String // encoded polyline
)

@Serializable
data class LatLngBounds(
    val northeast: LatLng,
    val southwest: LatLng
)

/**
 * Decode Google polyline to list of coordinates.
 * Algorithm: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
fun Polyline.decode(): List<LatLng> {
    val len = points.length
    var index = 0
    val coordinates = mutableListOf<LatLng>()
    var lat = 0
    var lng = 0

    while (index < len) {
        var result = 1
        var shift = 0
        var b: Int
        do {
            b = points[index++].code - 63 - 1
            result += b shl shift
            shift += 5
        } while (b >= 0x1f)
        lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

        result = 1
        shift = 0
        do {
            b = points[index++].code - 63 - 1
            result += b shl shift
            shift += 5
        } while (b >= 0x1f)
        lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

        coordinates.add(LatLng(lat / 1e5, lng / 1e5))
    }

    return coordinates
}

