package com.trailguide.android.data.dto

/**
 * Request body for registering device FCM tokens with the backend.
 */
data class FcmTokenRequest(
    val userId: String,
    val fcmToken: String,
    val deviceInfo: String
)

