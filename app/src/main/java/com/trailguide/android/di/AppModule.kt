package com.trailguide.android.di

import android.content.Context
import com.trailguide.android.BuildConfig
import com.trailguide.android.data.local.DownloadedTrailDao
import com.trailguide.android.data.local.TrailDatabase
import com.trailguide.android.data.local.ReviewDao
import com.trailguide.android.data.local.CollectionDao
import com.trailguide.android.data.remote.ApiClient
import com.trailguide.android.data.remote.AuthApiService
import com.trailguide.android.data.remote.TrailApiService
import com.trailguide.android.data.remote.WeatherApiService
import com.trailguide.android.data.notification.NotificationScheduler
import com.trailguide.android.data.repository.AuthRepository
import com.trailguide.android.data.repository.LocationRepository
import com.trailguide.android.data.repository.PreferencesRepository
import com.trailguide.android.data.repository.TrailRepository
import com.trailguide.android.data.repository.WeatherRepository
import com.trailguide.android.data.security.BiometricAuthenticationManager
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
     * Provides Trail Repository.
     */
    @Provides
    @Singleton
    fun provideTrailRepository(
        apiService: TrailApiService,
        downloadedTrailDao: DownloadedTrailDao,
        supabaseClient: SupabaseClient
    ): TrailRepository {
        return TrailRepository(apiService, downloadedTrailDao, supabaseClient)
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
        biometricAuthManager: BiometricAuthenticationManager
    ): AuthRepository {
        return AuthRepository(supabaseClient, biometricAuthManager)
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
     * Uses Google Weather API v1 endpoint
     */
    @Provides
    @Singleton
    fun provideWeatherApiService(): WeatherApiService {
        return Retrofit.Builder()
            .baseUrl("https://weather.googleapis.com/v1/")
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
        weatherApiService: WeatherApiService,
        preferencesRepository: PreferencesRepository
    ): WeatherRepository {
        return WeatherRepository(weatherApiService, preferencesRepository)
    }
    
    /**
     * Provides Location Repository.
     */
    @Provides
    @Singleton
    fun provideLocationRepository(
        @ApplicationContext context: Context
    ): LocationRepository {
        return LocationRepository(context)
    }
    
    /**
     * Provides Notification Scheduler.
     */
    @Provides
    @Singleton
    fun provideNotificationScheduler(
        @ApplicationContext context: Context
    ): NotificationScheduler {
        return NotificationScheduler(context)
    }
}
