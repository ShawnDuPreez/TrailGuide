package com.trailguide.android.di

import com.trailguide.android.data.osm.OverpassApiService
import com.trailguide.android.data.repository.OsmTrailRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing OpenStreetMap related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object OsmModule {
    
    @Provides
    @Singleton
    fun provideOverpassApiService(): OverpassApiService {
        return OverpassApiService()
    }
    
    @Provides
    @Singleton
    fun provideOsmTrailRepository(
        overpassApiService: OverpassApiService
    ): OsmTrailRepository {
        return OsmTrailRepository(overpassApiService)
    }
}
