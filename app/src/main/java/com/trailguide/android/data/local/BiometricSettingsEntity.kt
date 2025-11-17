package com.trailguide.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing biometric authentication settings per email.
 * Stores whether biometric authentication is enabled for a specific email address.
 */
@Entity(tableName = "biometric_settings")
data class BiometricSettingsEntity(
    @PrimaryKey
    val email: String, // Normalized lowercase email as primary key
    val biometricEnabled: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

