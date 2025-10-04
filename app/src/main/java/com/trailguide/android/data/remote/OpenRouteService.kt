package com.trailguide.android.data.remote

import com.trailguide.android.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * OpenRouteService API for generating hiking routes
 * Free tier: 2,000 requests/day
 */
interface OpenRouteServiceApi {
    
    /**
     * Get directions (route) between coordinates
     * @param coordinates Format: "start_lon,start_lat|end_lon,end_lat" 
     * @param profile Walking profile: "foot-hiking"
     */
    @GET("v2/directions/foot-hiking")
    suspend fun getRoute(
        @Query("api_key") apiKey: String = BuildConfig.OPENROUTE_API_KEY,
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<OpenRouteResponse>
    
    /**
     * Simplified route request using GET with coordinates
     */
    @GET("v2/directions/foot-hiking/geojson")
    suspend fun getRouteGeoJson(
        @Query("api_key") apiKey: String = BuildConfig.OPENROUTE_API_KEY,
        @Query("start") start: String, // "lon,lat"
        @Query("end") end: String      // "lon,lat"
    ): Response<OpenRouteGeoJsonResponse>
}

// Response models
data class OpenRouteResponse(
    val features: List<RouteFeature>
)

data class RouteFeature(
    val geometry: RouteGeometry,
    val properties: RouteProperties
)

data class RouteGeometry(
    val coordinates: List<List<Double>>, // [[lon, lat, elevation], ...]
    val type: String
)

data class RouteProperties(
    val segments: List<RouteSegment>,
    val summary: RouteSummary
)

data class RouteSegment(
    val distance: Double,
    val duration: Double,
    val steps: List<RouteStep>?
)

data class RouteStep(
    val distance: Double,
    val duration: Double,
    val type: Int,
    val instruction: String
)

data class RouteSummary(
    val distance: Double,
    val duration: Double
)

// GeoJSON response (simpler)
data class OpenRouteGeoJsonResponse(
    val type: String,
    val features: List<GeoJsonFeature>
)

data class GeoJsonFeature(
    val type: String,
    val geometry: GeoJsonGeometry,
    val properties: Map<String, Any>?
)

data class GeoJsonGeometry(
    val type: String,
    val coordinates: List<List<Double>> // [[lon, lat], [lon, lat], ...]
)

/**
 * Singleton client for OpenRouteService API
 */
object OpenRouteClient {
    private const val BASE_URL = "https://api.openrouteservice.org/"
    private const val TIMEOUT_SECONDS = 30L
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: OpenRouteServiceApi = retrofit.create(OpenRouteServiceApi::class.java)
}

