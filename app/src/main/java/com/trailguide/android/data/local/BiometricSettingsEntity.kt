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
    val email: String,
    val biometricEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun create(
            email: String,
            biometricEnabled: Boolean,
            createdAt: Long = System.currentTimeMillis(),
            updatedAt: Long = System.currentTimeMillis()
        ): BiometricSettingsEntity {
            val normalizedEmail = email.trim().lowercase()
            return BiometricSettingsEntity(
                email = normalizedEmail,
                biometricEnabled = biometricEnabled,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}

