package com.trailguide.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for offline trail storage.
 * Stores downloaded trails for offline access.
 */
@Database(
    entities = [
        DownloadedTrailEntity::class,
        ReviewEntity::class,
        CollectionEntity::class,
        FavoriteTrailEntity::class,
        TrailProgressEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrailDatabase : RoomDatabase() {
    
    abstract fun downloadedTrailDao(): DownloadedTrailDao
    abstract fun reviewDao(): ReviewDao
    abstract fun collectionDao(): CollectionDao
    abstract fun favoriteTrailDao(): FavoriteTrailDao
    abstract fun trailProgressDao(): TrailProgressDao
    
    companion object {
        @Volatile
        private var INSTANCE: TrailDatabase? = null
        
        fun getDatabase(context: Context): TrailDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrailDatabase::class.java,
                    "trail_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

