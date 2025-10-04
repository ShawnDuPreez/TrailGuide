package com.trailguide.android.data.remote

import com.trailguide.android.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object for creating and configuring Retrofit instances.
 * Provides HTTP clients with logging, authentication, and timeout configurations.
 */
object ApiClient {
    
    // Extended timeout for Render.com free tier cold starts (can take 60-90 seconds)
    private const val TIMEOUT_SECONDS = 120L
    
    /**
     * Auth token for authenticated requests.
     * This should be set after successful login.
     */
    var authToken: String? = null
    
    /**
     * Logging interceptor for debugging HTTP requests/responses.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    /**
     * Auth interceptor to add Bearer token to requests.
     */
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder().apply {
            authToken?.let { token ->
                addHeader("Authorization", "Bearer $token")
            }
            addHeader("Content-Type", "application/json")
            addHeader("Accept", "application/json")
        }.build()
        chain.proceed(request)
    }
    
    /**
     * OkHttp client with interceptors and timeout configuration.
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Retrofit instance configured with base URL and converters.
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Trail API service instance.
     */
    val trailApiService: TrailApiService by lazy {
        retrofit.create(TrailApiService::class.java)
    }
    
    /**
     * Auth API service instance.
     */
    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}

