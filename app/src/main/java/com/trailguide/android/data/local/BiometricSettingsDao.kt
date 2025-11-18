package com.trailguide.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

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
    suspend fun upsert(settings: BiometricSettingsEntity)
    
    /**
     * Set biometric enabled state for an email.
     * Creates new entry if doesn't exist, updates if it does.
     */
    @Transaction
    suspend fun setBiometricEnabled(email: String, enabled: Boolean) {
        val normalizedEmail = email.trim().lowercase()
        val existing = getBiometricSettings(normalizedEmail)
        val now = System.currentTimeMillis()
        val entity = existing
            ?.copy(
                biometricEnabled = enabled,
                updatedAt = now
            )
            ?: BiometricSettingsEntity.create(
                email = normalizedEmail,
                biometricEnabled = enabled,
                createdAt = now,
                updatedAt = now
            )
        upsert(entity)
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
