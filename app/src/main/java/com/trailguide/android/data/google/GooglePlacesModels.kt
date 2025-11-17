package com.trailguide.android.data.google

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Google Places API response models for hiking trail discovery.
 */

@Serializable
data class PlacesSearchResponse(
    val results: List<PlaceResult> = emptyList(),
    val status: String,
    @SerialName("error_message")
    val errorMessage: String? = null,
    @SerialName("next_page_token")
    val nextPageToken: String? = null
)

@Serializable
data class PlaceResult(
    @SerialName("place_id")
    val placeId: String,
    val name: String,
    val vicinity: String? = null,
    val geometry: PlaceGeometry,
    val rating: Double? = null,
    @SerialName("user_ratings_total")
    val userRatingsTotal: Int? = null,
    val photos: List<PlacePhoto>? = null,
    val types: List<String> = emptyList(),
    @SerialName("opening_hours")
    val openingHours: PlaceOpeningHours? = null
)

@Serializable
data class PlaceGeometry(
    val location: PlaceLocation,
    val viewport: PlaceViewport? = null
)

@Serializable
data class PlaceLocation(
    val lat: Double,
    val lng: Double
)

@Serializable
data class PlaceViewport(
    val northeast: PlaceLocation,
    val southwest: PlaceLocation
)

@Serializable
data class PlacePhoto(
    @SerialName("photo_reference")
    val photoReference: String,
    val height: Int,
    val width: Int,
    @SerialName("html_attributions")
    val htmlAttributions: List<String> = emptyList()
)

@Serializable
data class PlaceOpeningHours(
    @SerialName("open_now")
    val openNow: Boolean? = null
)

// Place Details API Response
@Serializable
data class PlaceDetailsResponse(
    val result: PlaceDetails,
    val status: String,
    @SerialName("error_message")
    val errorMessage: String? = null
)

@Serializable
data class PlaceDetails(
    @SerialName("place_id")
    val placeId: String,
    val name: String,
    @SerialName("formatted_address")
    val formattedAddress: String? = null,
    @SerialName("formatted_phone_number")
    val formattedPhoneNumber: String? = null,
    val geometry: PlaceGeometry,
    val rating: Double? = null,
    @SerialName("user_ratings_total")
    val userRatingsTotal: Int? = null,
    val photos: List<PlacePhoto>? = null,
    val reviews: List<PlaceReview>? = null,
    val website: String? = null,
    @SerialName("opening_hours")
    val openingHours: PlaceDetailedOpeningHours? = null,
    val types: List<String> = emptyList()
)

@Serializable
data class PlaceReview(
    @SerialName("author_name")
    val authorName: String,
    val rating: Int,
    @SerialName("relative_time_description")
    val relativeTimeDescription: String,
    val text: String,
    val time: Long
)

@Serializable
data class PlaceDetailedOpeningHours(
    @SerialName("open_now")
    val openNow: Boolean? = null,
    @SerialName("weekday_text")
    val weekdayText: List<String>? = null
)

/**
 * Build photo URL from photo reference.
 */
fun PlacePhoto.buildPhotoUrl(apiKey: String, maxWidth: Int = 400): String {
    return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=$maxWidth&photo_reference=$photoReference&key=$apiKey"
}

