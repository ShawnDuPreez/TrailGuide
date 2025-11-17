package com.trailguide.android.data.repository

import android.util.Log
import com.trailguide.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing translation operations using Google Translate API.
 * Handles text translation, language detection, and batch translations.
 */
@Singleton
class TranslationRepository @Inject constructor() {
    
    companion object {
        private const val TAG = "TranslationRepository"
        private const val TRANSLATE_API_URL = "https://translation.googleapis.com/language/translate/v2"
        private const val DETECT_API_URL = "https://translation.googleapis.com/language/translate/v2/detect"
    }
    
    private val apiKey = BuildConfig.GOOGLE_TRANSLATE_API_KEY
    private val translationCache = mutableMapOf<String, String>()
    
    /**
     * Check if the API is configured.
     */
    fun isConfigured(): Boolean {
        return apiKey.isNotEmpty()
    }
    
    /**
     * Translate a single text to target language.
     */
    suspend fun translateText(
        text: String,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Translation API not configured"))
            }
            
            if (text.isBlank()) {
                return@withContext Result.success(text)
            }
            
            // Check cache
            val cacheKey = "$text|$targetLanguage|$sourceLanguage"
            translationCache[cacheKey]?.let {
                return@withContext Result.success(it)
            }
            
            // Build URL
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = buildString {
                append("$TRANSLATE_API_URL?key=$apiKey")
                append("&q=$encodedText")
                append("&target=$targetLanguage")
                if (sourceLanguage != null) {
                    append("&source=$sourceLanguage")
                }
            }
            
            // Make API call
            val response = URL(url).readText()
            val jsonResponse = JSONObject(response)
            
            // Parse response
            val translationsArray = jsonResponse
                .getJSONObject("data")
                .getJSONArray("translations")
            
            val translatedText = translationsArray
                .getJSONObject(0)
                .getString("translatedText")
            
            // Cache result
            translationCache[cacheKey] = translatedText
            
            Log.d(TAG, "Translation successful: $text -> $translatedText")
            Result.success(translatedText)
        } catch (e: Exception) {
            Log.e(TAG, "Translation error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Translate multiple texts in a batch.
     */
    suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Translation API not configured"))
            }
            
            if (texts.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            // For simplicity, translate one by one
            // In production, you could optimize this with batch API calls
            val translations = texts.map { text ->
                translateText(text, targetLanguage, sourceLanguage)
                    .getOrElse { text } // Return original on failure
            }
            
            Result.success(translations)
        } catch (e: Exception) {
            Log.e(TAG, "Batch translation error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Detect the language of a text.
     */
    suspend fun detectLanguage(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("Translation API not configured"))
            }
            
            if (text.isBlank()) {
                return@withContext Result.success("en")
            }
            
            // Build URL
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = "$DETECT_API_URL?key=$apiKey&q=$encodedText"
            
            // Make API call
            val response = URL(url).readText()
            val jsonResponse = JSONObject(response)
            
            // Parse response
            val detectionsArray = jsonResponse
                .getJSONObject("data")
                .getJSONArray("detections")
                .getJSONArray(0)
            
            val language = detectionsArray
                .getJSONObject(0)
                .getString("language")
            
            Log.d(TAG, "Language detected: $language")
            Result.success(language)
        } catch (e: Exception) {
            Log.e(TAG, "Language detection error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clear the translation cache.
     */
    fun clearCache() {
        translationCache.clear()
        Log.d(TAG, "Translation cache cleared")
    }
}

