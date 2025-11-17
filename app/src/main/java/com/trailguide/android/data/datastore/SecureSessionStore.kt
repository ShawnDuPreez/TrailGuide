package com.trailguide.android.data.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for Supabase session tokens using EncryptedSharedPreferences.
 * Stores access token, refresh token, and token expiry time.
 */
@Singleton
class SecureSessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "secure_session_store"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
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
    
    /**
     * Store session tokens securely.
     */
    fun storeSession(
        accessToken: String,
        refreshToken: String,
        expiresAt: Long,
        userEmail: String,
        userId: String
    ) {
        encryptedPrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_TOKEN_EXPIRY, expiresAt)
            putString(KEY_USER_EMAIL, userEmail)
            putString(KEY_USER_ID, userId)
            apply()
        }
    }
    
    /**
     * Retrieve access token.
     */
    fun getAccessToken(): String? {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Retrieve refresh token.
     */
    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }
    
    /**
     * Get token expiry time.
     */
    fun getTokenExpiry(): Long {
        return encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
    }
    
    /**
     * Get stored user email.
     */
    fun getUserEmail(): String? {
        return encryptedPrefs.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Get stored user ID.
     */
    fun getUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Check if session is stored and valid.
     */
    fun hasValidSession(): Boolean {
        val accessToken = getAccessToken()
        val expiry = getTokenExpiry()
        val currentTime = System.currentTimeMillis() / 1000
        
        return !accessToken.isNullOrEmpty() && expiry > currentTime
    }
    
    /**
     * Check if session exists (even if expired).
     */
    fun hasSession(): Boolean {
        return getAccessToken() != null
    }
    
    /**
     * Clear all stored session data.
     */
    fun clearSession() {
        clearSession(keepTokens = false)
    }
    
    /**
     * Clear session data with option to keep tokens for biometric login.
     * @param keepTokens If true, only clears user info, keeps access/refresh tokens
     */
    fun clearSession(keepTokens: Boolean) {
        encryptedPrefs.edit().apply {
            if (keepTokens) {
                // Only clear user info, keep tokens for biometric login
                remove(KEY_USER_EMAIL)
                remove(KEY_USER_ID)
            } else {
                // Clear everything
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_TOKEN_EXPIRY)
                remove(KEY_USER_EMAIL)
                remove(KEY_USER_ID)
            }
            apply()
        }
    }
    
    /**
     * Set biometric authentication enabled status.
     */
    fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }
    
    /**
     * Check if biometric authentication is enabled.
     */
    fun isBiometricEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    /**
     * Clear all data including biometric settings.
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
}

