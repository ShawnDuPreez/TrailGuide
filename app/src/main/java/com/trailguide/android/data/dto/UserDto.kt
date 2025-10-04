package com.trailguide.android.data.dto

import com.google.gson.annotations.SerializedName

/**
 * User authentication response DTO.
 */
data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("provider") val provider: String?
)

/**
 * Login request DTO.
 */
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/**
 * Registration request DTO.
 */
data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("display_name") val displayName: String
)

/**
 * OAuth token response DTO.
 */
data class OAuthTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("user") val user: UserDto
)

