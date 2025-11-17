package com.trailguide.android.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Google Cloud Translation API service.
 * Provides real-time translation capabilities for the app.
 */
interface GoogleTranslateApiService {
    
    @POST("language/translate/v2")
    suspend fun translateText(
        @Query("key") apiKey: String,
        @Body request: TranslateRequest
    ): TranslateResponse
    
    @POST("language/translate/v2/detect")
    suspend fun detectLanguage(
        @Query("key") apiKey: String,
        @Body request: DetectLanguageRequest
    ): DetectLanguageResponse
}

// Request/Response models
data class TranslateRequest(
    val q: List<String>,
    val target: String,
    val source: String? = null,
    val format: String = "text"
)

data class TranslateResponse(
    val data: TranslateData
)

data class TranslateData(
    val translations: List<Translation>
)

data class Translation(
    val translatedText: String,
    val detectedSourceLanguage: String? = null
)

data class DetectLanguageRequest(
    val q: List<String>
)

data class DetectLanguageResponse(
    val data: DetectLanguageData
)

data class DetectLanguageData(
    val detections: List<List<LanguageDetection>>
)

data class LanguageDetection(
    val language: String,
    val isReliable: Boolean,
    val confidence: Float
)
