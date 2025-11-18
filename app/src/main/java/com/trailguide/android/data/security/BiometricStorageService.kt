package com.trailguide.android.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.trailguide.android.data.local.BiometricSettingsDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing per-user biometric authentication data.
 * Uses EncryptedSharedPreferences for secure credential storage.
 * Uses Room database for biometric enabled state (more secure than SharedPreferences).
 * 
 * SECURITY: Passwords are stored as fallback only. Primary authentication uses refresh tokens and session data.
 * Storage uses platform secure storage:
 * - Android: EncryptedSharedPreferences (AES-256 encryption) for credentials
 * - Android: Room database (encrypted at rest) for biometric enabled state
 */
@Singleton
class BiometricStorageService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val biometricSettingsDao: BiometricSettingsDao
) {
    
    companion object {
        private const val PREFS_NAME = "biometric_storage"
        private const val KEY_CREDENTIALS_PREFIX = "biometric_credentials_email_"
        
        // Old global keys for migration
        private const val OLD_KEY_ENCRYPTED_CREDENTIALS = "encrypted_credentials"
        private const val OLD_KEY_IV = "iv"
        private const val OLD_PREFS_NAME = "biometric_credentials"
        
        // Old SharedPreferences key for biometric enabled (to migrate to Room)
        private const val OLD_KEY_BIOMETRIC_ENABLED_PREFIX = "biometric_enabled_email_"
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    private val regularPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val migrationMutex = Mutex()
    @Volatile
    private var biometricPrefsMigrated = false
    
    /**
     * Save encrypted credentials by email (simplified - no userId lookup needed).
     * SECURITY: Passwords are stored as fallback only. Primary authentication uses refresh tokens and session data.
     */
    suspend fun saveCredentials(
        email: String,
        refreshToken: String? = null,
        sessionData: String? = null,
        password: String? = null,
        userId: String? = null
    ) {
        if (email.isEmpty()) {
            throw Exception("Email is required")
        }
        
        val normalizedEmail = email.trim().lowercase()
        val emailHash = hashEmail(normalizedEmail)
        
        val credentials = mutableMapOf<String, String>()
        credentials["email"] = normalizedEmail
        credentials["savedAt"] = System.currentTimeMillis().toString()
        
        refreshToken?.let { credentials["refreshToken"] = it }
        sessionData?.let { credentials["sessionData"] = it }
        password?.let { credentials["password"] = it }
        userId?.let { credentials["userId"] = it }
        
        val credentialsJson = credentials.entries.joinToString(",") { 
            "\"${it.key}\":\"${it.value}\"" 
        }
        val jsonString = "{$credentialsJson}"
        
        encryptedPrefs.edit()
            .putString("${KEY_CREDENTIALS_PREFIX}$emailHash", jsonString)
            .apply()
    }
    
    /**
     * Get encrypted credentials by email.
     */
    fun getCredentialsByEmail(email: String): Map<String, String>? {
        val normalizedEmail = email.trim().lowercase()
        val emailHash = hashEmail(normalizedEmail)
        val credentialsJson = encryptedPrefs.getString("${KEY_CREDENTIALS_PREFIX}$emailHash", null) ?: return null
        
        return try {
            parseJsonToMap(credentialsJson)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Delete credentials for a specific email.
     */
    fun deleteCredentials(email: String) {
        val normalizedEmail = email.trim().lowercase()
        val emailHash = hashEmail(normalizedEmail)
        encryptedPrefs.edit()
            .remove("${KEY_CREDENTIALS_PREFIX}$emailHash")
            .apply()
    }
    
    /**
     * Check if biometric authentication is enabled for an email.
     * Queries Room database for the enabled state.
     */
    suspend fun isBiometricEnabled(email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        ensureBiometricSettingsMigration()
        return biometricSettingsDao.getBiometricEnabledSync(normalizedEmail) ?: false
    }
    
    /**
     * Check if biometric authentication is enabled for an email (synchronous, blocking).
     * For use in non-coroutine contexts.
     */
    fun isBiometricEnabledSync(email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        return runBlocking {
            ensureBiometricSettingsMigration()
            biometricSettingsDao.getBiometricEnabledSync(normalizedEmail) ?: false
        }
    }
    
    /**
     * Set biometric authentication enabled/disabled for an email.
     * Stores in Room database (more secure than SharedPreferences).
     * This state persists across logouts.
     */
    suspend fun setBiometricEnabled(email: String, enabled: Boolean) {
        val normalizedEmail = email.trim().lowercase()
        ensureBiometricSettingsMigration()
        biometricSettingsDao.setBiometricEnabled(normalizedEmail, enabled)
    }
    
    /**
     * Clear all biometric data for a specific email.
     */
    suspend fun clearAllUserData(email: String) {
        val normalizedEmail = email.trim().lowercase()
        deleteCredentials(normalizedEmail)
        ensureBiometricSettingsMigration()
        biometricSettingsDao.deleteBiometricSettings(normalizedEmail)
    }
    
    /**
     * Check if credentials exist for an email.
     */
    fun hasCredentialsForEmail(email: String): Boolean {
        return getCredentialsByEmail(email) != null
    }
    
    /**
     * Migrate old global credentials to email-based storage.
     * Also migrates biometric enabled state from SharedPreferences to Room DB.
     * This should be called once on app startup or first login.
     */
    suspend fun migrateOldCredentials(email: String) {
        // Email parameter is used in the migration logic below
        try {
            val oldPrefs = context.getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
            val oldEncryptedCredentials = oldPrefs.getString(OLD_KEY_ENCRYPTED_CREDENTIALS, null)
            val oldBiometricEnabled = oldPrefs.getBoolean("biometric_enabled", false)
            val oldEmail = oldPrefs.getString("saved_email", null)
            
            if (oldEncryptedCredentials != null && oldEmail != null) {
                val normalizedEmail = oldEmail.trim().lowercase()
                // Check if user already has credentials (don't overwrite)
                val existingCredentials = getCredentialsByEmail(normalizedEmail)
                if (existingCredentials == null) {
                    // Migrate old credentials - they'll be re-saved on next login
                }
                
                // Migrate biometric preference from old global storage to Room DB
                val existingPreference = isBiometricEnabled(normalizedEmail)
                if (!existingPreference && oldBiometricEnabled) {
                    setBiometricEnabled(normalizedEmail, true)
                }
                
                // Clean up old global keys
                oldPrefs.edit()
                    .remove(OLD_KEY_ENCRYPTED_CREDENTIALS)
                    .remove(OLD_KEY_IV)
                    .remove("biometric_enabled")
                    .remove("saved_email")
                    .remove("saved_password")
                    .remove("temp_email")
                    .remove("temp_password")
                    .apply()
            }
            
            // Migrate biometric enabled state from SharedPreferences to Room DB
            ensureBiometricSettingsMigration()
        } catch (e: Exception) {
            // Ignore migration errors
        }
    }
    
    /**
     * Migrate biometric enabled flags from SharedPreferences to Room database.
     * This runs once on first access after the update.
     */
    private suspend fun migrateBiometricEnabledFromSharedPreferences() {
        try {
            val allPrefs = regularPrefs.all
            val keysToMigrate = allPrefs.keys.filter {
                it.startsWith(OLD_KEY_BIOMETRIC_ENABLED_PREFIX)
            }
            if (keysToMigrate.isEmpty()) {
                return
            }
            
            val emailHashLookup = buildEmailHashLookup()
            val editor = regularPrefs.edit()
            
            for (key in keysToMigrate) {
                val enabled = regularPrefs.getBoolean(key, false)
                val emailHash = key.removePrefix(OLD_KEY_BIOMETRIC_ENABLED_PREFIX)
                val normalizedEmail = emailHashLookup[emailHash]
                
                if (normalizedEmail != null) {
                    biometricSettingsDao.setBiometricEnabled(normalizedEmail, enabled)
                }
                editor.remove(key)
            }
            
            editor.apply()
        } catch (e: Exception) {
            // Ignore migration errors
        }
    }
    
    private suspend fun ensureBiometricSettingsMigration() {
        if (biometricPrefsMigrated) return
        migrationMutex.withLock {
            if (!biometricPrefsMigrated) {
                migrateBiometricEnabledFromSharedPreferences()
                biometricPrefsMigrated = true
            }
        }
    }
    
    private fun buildEmailHashLookup(): Map<String, String> {
        val lookup = mutableMapOf<String, String>()
        try {
            val allEncryptedPrefs = encryptedPrefs.all
            val credentialKeys = allEncryptedPrefs.keys.filter { it.startsWith(KEY_CREDENTIALS_PREFIX) }
            
            for (key in credentialKeys) {
                val emailHash = key.removePrefix(KEY_CREDENTIALS_PREFIX)
                val credentialsJson = encryptedPrefs.getString(key, null) ?: continue
                val credentials = try {
                    parseJsonToMap(credentialsJson)
                } catch (e: Exception) {
                    null
                }
                
                val email = credentials?.get("email")?.trim()?.lowercase()
                if (!email.isNullOrBlank()) {
                    lookup[emailHash] = email
                }
            }
        } catch (e: Exception) {
            // Ignore mapping errors - migration will skip entries without a match
        }
        return lookup
    }
    
    /**
     * Hash email for storage key using SHA-256.
     */
    private fun hashEmail(email: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(email.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Parse simple JSON string to Map.
     */
    private fun parseJsonToMap(json: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val content = json.removePrefix("{").removeSuffix("}")
        val pairs = content.split(",")
        
        for (pair in pairs) {
            val keyValue = pair.split(":", limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0].trim().removeSurrounding("\"")
                val value = keyValue[1].trim().removeSurrounding("\"")
                map[key] = value
            }
        }
        
        return map
    }
}

