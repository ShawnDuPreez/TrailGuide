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
    entities = [DownloadedTrailEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrailDatabase : RoomDatabase() {
    
    abstract fun downloadedTrailDao(): DownloadedTrailDao
    
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

