package com.trailguide.android.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages biometric authentication using Android's BiometricPrompt API.
 * Provides secure storage and retrieval of user credentials using Android Keystore.
 */
class BiometricAuthenticationManager(
    private val context: Context
) {
    
    companion object {
        private const val KEY_ALIAS_PREFIX = "TrailGuideBiometricKey_"
        private const val KEYSTORE_NAME = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TAG = "BiometricAuth"
        
        // Legacy key alias for migration
        private const val LEGACY_KEY_ALIAS = "TrailGuideBiometricKey"
    }
    
    private val biometricManager = BiometricManager.from(context)
    
    /**
     * Check if biometric authentication is available on the device.
     */
    fun isBiometricAvailable(): BiometricStatus {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNKNOWN_ERROR
        }
    }
    
    /**
     * Check if biometric authentication is available and enrolled.
     */
    fun canUseBiometric(): Boolean {
        return isBiometricAvailable() == BiometricStatus.AVAILABLE
    }
    
    /**
     * Prompt user for biometric authentication.
     * @param activity The FragmentActivity to show the biometric prompt
     * @param title Title for the biometric prompt
     * @param subtitle Subtitle for the biometric prompt
     * @return true if authentication was successful, false otherwise
     */
    suspend fun authenticateWithBiometric(
        activity: FragmentActivity,
        title: String = "Authenticate",
        subtitle: String = "Use your biometric to authenticate",
        negativeButtonText: String = "Cancel"
    ): Boolean = suspendCancellableCoroutine { continuation ->
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
            
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                if (continuation.isActive) {
                    continuation.resume(true)
                }
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Don't resume here - let user try again
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
        
        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
    }
    
    /**
     * Encrypt and store user credentials securely using biometric authentication.
     * @param activity The FragmentActivity to show the biometric prompt
     * @param userId User's ID for per-user key storage
     * @param email User's email
     * @param password User's password (will be encrypted)
     * @return true if credentials were stored successfully, false otherwise
     */
    suspend fun storeCredentialsWithBiometric(
        activity: FragmentActivity,
        userId: String,
        email: String,
        password: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        
        if (!canUseBiometric()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        try {
            // Generate or get the encryption key for this user
            val secretKey = generateSecretKey(userId)
            
            // Encrypt the credentials
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedCredentials = cipher.doFinal("$email:$password".toByteArray())
            
            // Store encrypted data per user
            val sharedPrefs = context.getSharedPreferences("biometric_credentials", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            editor.putString("encrypted_credentials_$userId", android.util.Base64.encodeToString(encryptedCredentials, android.util.Base64.DEFAULT))
            editor.putString("iv_$userId", android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT))
            editor.apply()
            
            continuation.resume(true)
            
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Retrieve and decrypt user credentials using biometric authentication.
     * @param activity The FragmentActivity to show the biometric prompt
     * @param userId User's ID for per-user key storage
     * @return Pair of (email, password) if successful, null otherwise
     */
    suspend fun retrieveCredentialsWithBiometric(
        activity: FragmentActivity,
        userId: String
    ): Pair<String, String>? = suspendCancellableCoroutine { continuation ->
        
        if (!canUseBiometric()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        try {
            val sharedPrefs = context.getSharedPreferences("biometric_credentials", Context.MODE_PRIVATE)
            val encryptedCredentials = sharedPrefs.getString("encrypted_credentials_$userId", null)
            val ivString = sharedPrefs.getString("iv_$userId", null)
            
            if (encryptedCredentials == null || ivString == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            
            val secretKey = getSecretKey(userId)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = android.util.Base64.decode(ivString, android.util.Base64.DEFAULT)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            
            val decryptedBytes = cipher.doFinal(android.util.Base64.decode(encryptedCredentials, android.util.Base64.DEFAULT))
            val credentials = String(decryptedBytes)
            
            val parts = credentials.split(":", limit = 2)
            if (parts.size == 2) {
                continuation.resume(Pair(parts[0], parts[1]))
            } else {
                continuation.resume(null)
            }
            
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Clear stored biometric credentials for a specific user.
     */
    fun clearStoredCredentials(userId: String) {
        val sharedPrefs = context.getSharedPreferences("biometric_credentials", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .remove("encrypted_credentials_$userId")
            .remove("iv_$userId")
            .apply()
    }
    
    /**
     * Check if biometric credentials are stored for a specific user.
     */
    fun hasStoredCredentials(userId: String): Boolean {
        val sharedPrefs = context.getSharedPreferences("biometric_credentials", Context.MODE_PRIVATE)
        return sharedPrefs.getString("encrypted_credentials_$userId", null) != null
    }
    
    private fun generateSecretKey(userId: String): SecretKey {
        val keyAlias = "$KEY_ALIAS_PREFIX$userId"
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(-1) // Require auth for every use
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    private fun getSecretKey(userId: String): SecretKey {
        val keyAlias = "$KEY_ALIAS_PREFIX$userId"
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        // Try per-user key first
        return try {
            keyStore.getKey(keyAlias, null) as SecretKey
        } catch (e: Exception) {
            // Fallback to legacy key for migration
            try {
                keyStore.getKey(LEGACY_KEY_ALIAS, null) as SecretKey
            } catch (e2: Exception) {
                throw Exception("No biometric key found for user $userId")
            }
        }
    }
}

/**
 * Status of biometric authentication availability.
 */
enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    UNKNOWN_ERROR
}
