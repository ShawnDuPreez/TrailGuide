package com.trailguide.android.di

import android.content.Context
import com.trailguide.android.BuildConfig
import com.trailguide.android.data.remote.ApiClient
import com.trailguide.android.data.remote.AuthApiService
import com.trailguide.android.data.remote.TrailApiService
import com.trailguide.android.data.repository.AuthRepository
import com.trailguide.android.data.repository.PreferencesRepository
import com.trailguide.android.data.repository.TrailRepository
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
     * Provides Trail Repository.
     */
    @Provides
    @Singleton
    fun provideTrailRepository(
        apiService: TrailApiService
    ): TrailRepository {
        return TrailRepository(apiService)
    }
    
    /**
     * Provides Auth Repository with Supabase client.
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        supabaseClient: SupabaseClient
    ): AuthRepository {
        return AuthRepository(supabaseClient)
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
}
