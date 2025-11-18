package com.trailguide.android.di

import android.content.Context
import com.trailguide.android.BuildConfig
import com.trailguide.android.data.local.DownloadedTrailDao
import com.trailguide.android.data.local.TrailDatabase
import com.trailguide.android.data.local.ReviewDao
import com.trailguide.android.data.local.CollectionDao
import com.trailguide.android.data.google.GoogleDirectionsApiService
import com.trailguide.android.data.google.GoogleElevationApiService
import com.trailguide.android.data.google.GooglePlacesApiService
import com.trailguide.android.data.remote.ApiClient
import com.trailguide.android.data.remote.AuthApiService
import com.trailguide.android.data.remote.TrailApiService
import com.trailguide.android.data.remote.WeatherApiService
import com.trailguide.android.data.repository.AuthRepository
import com.trailguide.android.data.repository.PreferencesRepository
import com.trailguide.android.data.repository.TrailRepository
import com.trailguide.android.data.repository.WeatherRepository
import com.trailguide.android.data.security.BiometricAuthenticationManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

/**
 * Hilt module for providing application-level dependencies.
 * Configures dependency injection for repositories, API services, and Supabase.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provides Supabase client with Auth and Postgrest modules.
     */
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth) {
                scheme = "trailguide"
                host = "auth-callback"
            }
            install(Postgrest)
        }
    }
    
    /**
     * Provides Trail API service.
     */
    @Provides
    @Singleton
    fun provideTrailApiService(): TrailApiService {
        return ApiClient.trailApiService
    }
    
    /**
     * Provides Auth API service.
     */
    @Provides
    @Singleton
    fun provideAuthApiService(): AuthApiService {
        return ApiClient.authApiService
    }
    
    /**
     * Provides Room Database.
     */
    @Provides
    @Singleton
    fun provideTrailDatabase(
        @ApplicationContext context: Context
    ): TrailDatabase {
        return TrailDatabase.getDatabase(context)
    }
    
    /**
     * Provides Downloaded Trail DAO.
     */
    @Provides
    @Singleton
    fun provideDownloadedTrailDao(
        database: TrailDatabase
    ): DownloadedTrailDao {
        return database.downloadedTrailDao()
    }
    
    /**
     * Provides Review DAO.
     */
    @Provides
    @Singleton
    fun provideReviewDao(
        database: TrailDatabase
    ): ReviewDao {
        return database.reviewDao()
    }
    
    /**
     * Provides Collection DAO.
     */
    @Provides
    @Singleton
    fun provideCollectionDao(
        database: TrailDatabase
    ): CollectionDao {
        return database.collectionDao()
    }
    
    /**
     * Provides Favorite Trail DAO.
     */
    @Provides
    @Singleton
    fun provideFavoriteTrailDao(
        database: TrailDatabase
    ): com.trailguide.android.data.local.FavoriteTrailDao {
        return database.favoriteTrailDao()
    }
    
    /**
     * Provides Trail Progress DAO.
     */
    @Provides
    @Singleton
    fun provideTrailProgressDao(
        database: TrailDatabase
    ): com.trailguide.android.data.local.TrailProgressDao {
        return database.trailProgressDao()
    }
    
    /**
     * Provides Biometric Settings DAO.
     */
    @Provides
    @Singleton
    fun provideBiometricSettingsDao(
        database: TrailDatabase
    ): com.trailguide.android.data.local.BiometricSettingsDao {
        return database.biometricSettingsDao()
    }

    @Provides
    @Singleton
    fun provideNavigationSessionDao(
        database: TrailDatabase
    ): com.trailguide.android.data.local.NavigationSessionDao {
        return database.navigationSessionDao()
    }

    @Provides
    @Singleton
    fun provideNavigationWaypointDao(
        database: TrailDatabase
    ): com.trailguide.android.data.local.NavigationWaypointDao {
        return database.navigationWaypointDao()
    }
    
    /**
     * Provides Trail Repository.
     */
    @Provides
    @Singleton
    fun provideTrailRepository(
        apiService: TrailApiService,
        downloadedTrailDao: DownloadedTrailDao,
        favoriteTrailDao: com.trailguide.android.data.local.FavoriteTrailDao,
        supabaseClient: SupabaseClient
    ): TrailRepository {
        return TrailRepository(apiService, downloadedTrailDao, favoriteTrailDao, supabaseClient)
    }
    
    /**
     * Provides Biometric Storage Service.
     */
    @Provides
    @Singleton
    fun provideBiometricStorageService(
        @ApplicationContext context: Context,
        biometricSettingsDao: com.trailguide.android.data.local.BiometricSettingsDao
    ): com.trailguide.android.data.security.BiometricStorageService {
        return com.trailguide.android.data.security.BiometricStorageService(context, biometricSettingsDao)
    }
    
    /**
     * Provides Biometric Authentication Manager.
     */
    @Provides
    @Singleton
    fun provideBiometricAuthenticationManager(
        @ApplicationContext context: Context
    ): BiometricAuthenticationManager {
        return BiometricAuthenticationManager(context)
    }
    
    /**
     * Provides Auth Repository with Supabase client.
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        supabaseClient: SupabaseClient,
        biometricAuthManager: BiometricAuthenticationManager,
        biometricStorageService: com.trailguide.android.data.security.BiometricStorageService,
        secureSessionStore: com.trailguide.android.data.datastore.SecureSessionStore
    ): AuthRepository {
        return AuthRepository(supabaseClient, biometricAuthManager, biometricStorageService, secureSessionStore)
    }
    
    /**
     * Provides Preferences Repository.
     */
    @Provides
    @Singleton
    fun providePreferencesRepository(
        @ApplicationContext context: Context
    ): PreferencesRepository {
        return PreferencesRepository(context)
    }
    
    /**
     * Provides Weather API service.
     */
    @Provides
    @Singleton
    fun provideWeatherApiService(): WeatherApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
    
    /**
     * Provides Weather Repository.
     */
    @Provides
    @Singleton
    fun provideWeatherRepository(
        weatherApiService: WeatherApiService
    ): WeatherRepository {
        return WeatherRepository(weatherApiService)
    }
    
    /**
     * Provides JSON serializer for Google APIs.
     */
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
    
    /**
     * Provides Google Places API service.
     */
    @Provides
    @Singleton
    fun provideGooglePlacesApiService(json: Json): GooglePlacesApiService {
        return Retrofit.Builder()
            .baseUrl(GooglePlacesApiService.BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApiService::class.java)
    }
    
    /**
     * Provides Google Directions API service.
     */
    @Provides
    @Singleton
    fun provideGoogleDirectionsApiService(json: Json): GoogleDirectionsApiService {
        return Retrofit.Builder()
            .baseUrl(GoogleDirectionsApiService.BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GoogleDirectionsApiService::class.java)
    }
    
    /**
     * Provides Google Elevation API service.
     */
    @Provides
    @Singleton
    fun provideGoogleElevationApiService(json: Json): GoogleElevationApiService {
        return Retrofit.Builder()
            .baseUrl(GoogleElevationApiService.BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GoogleElevationApiService::class.java)
    }
    
    /**
     * Provides Overpass API service for OSM trails.
     */
    @Provides
    @Singleton
    fun provideOverpassApiService(json: Json): com.trailguide.android.data.osm.OverpassApiService {
        return Retrofit.Builder()
            .baseUrl(com.trailguide.android.data.osm.OverpassApiService.BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(
                okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
            .build()
            .create(com.trailguide.android.data.osm.OverpassApiService::class.java)
    }
    
    /**
     * Provides Nominatim API service for OSM boundary searches.
     */
    @Provides
    @Singleton
    fun provideNominatimApiService(json: Json): com.trailguide.android.data.osm.NominatimApiService {
        return Retrofit.Builder()
            .baseUrl(com.trailguide.android.data.osm.NominatimApiService.BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(
                okhttp3.OkHttpClient.Builder()
                    .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        // Nominatim requires User-Agent header
                        val request = chain.request().newBuilder()
                            .addHeader("User-Agent", "TrailGuide-Android/1.1.0")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .build()
            .create(com.trailguide.android.data.osm.NominatimApiService::class.java)
    }
}
