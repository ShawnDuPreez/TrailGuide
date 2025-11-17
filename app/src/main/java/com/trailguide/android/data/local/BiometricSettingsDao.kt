package com.trailguide.android.data.local

import kotlinx.coroutines.flow.Flow
import androidx.room.*
import kotlinx.coroutines.flow.map

/**
 * DAO for biometric settings stored in Room database.
 */
@Dao
interface BiometricSettingsDao {
    
    /**
     * Get biometric enabled state for a specific email.
     * Returns Flow<Boolean?> - null if no setting exists, true/false if it does.
     */
    @Query("SELECT biometricEnabled FROM biometric_settings WHERE email = :email")
    fun getBiometricEnabled(email: String): Flow<Boolean?>
    
    /**
     * Get biometric enabled state synchronously (for non-coroutine contexts).
     */
    @Query("SELECT biometricEnabled FROM biometric_settings WHERE email = :email")
    suspend fun getBiometricEnabledSync(email: String): Boolean?
    
    /**
     * Insert or update biometric enabled state for an email.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: BiometricSettingsEntity)
    
    /**
     * Set biometric enabled state for an email.
     * Creates new entry if doesn't exist, updates if it does.
     */
    suspend fun setBiometricEnabled(email: String, enabled: Boolean) {
        val existing = getBiometricEnabledSync(email)
        val now = System.currentTimeMillis()
        
        if (existing != null) {
            // Update existing
            insertOrUpdate(
                BiometricSettingsEntity(
                    email = email,
                    biometricEnabled = enabled,
                    createdAt = now, // Keep original, but we'll need to query it first
                    updatedAt = now
                )
            )
        } else {
            // Create new
            insertOrUpdate(
                BiometricSettingsEntity(
                    email = email,
                    biometricEnabled = enabled,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
    
    /**
     * Delete biometric settings for a specific email.
     */
    @Query("DELETE FROM biometric_settings WHERE email = :email")
    suspend fun deleteBiometricSettings(email: String)
    
    /**
     * Get all biometric settings (for debugging/migration purposes).
     */
    @Query("SELECT * FROM biometric_settings")
    fun getAllBiometricSettings(): Flow<List<BiometricSettingsEntity>>
    
    /**
     * Get biometric settings entity for an email.
     */
    @Query("SELECT * FROM biometric_settings WHERE email = :email")
    suspend fun getBiometricSettings(email: String): BiometricSettingsEntity?
}

