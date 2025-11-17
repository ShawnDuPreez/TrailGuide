package com.trailguide.android.data.repository

import android.content.Context
import com.trailguide.android.data.osm.OsmBoundary
import com.trailguide.android.data.osm.OsmTrail
import com.trailguide.android.data.model.CombinedTrailStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles offline storage of trails and boundaries for download functionality
 */
@Singleton
class OfflineMapRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    private val offlineDir = File(context.getExternalFilesDir(null), "offline_maps")
    
    init {
        offlineDir.mkdirs()
    }
    
    /**
     * Save trails and boundary for offline use
     * Creates a complete offline package
     */
    suspend fun saveOfflineArea(
        areaName: String,
        boundary: OsmBoundary?,
        trails: List<OsmTrail>,
        combinedStats: CombinedTrailStats
    ): Flow<DownloadProgress> = flow {
        withContext(Dispatchers.IO) {
            try {
                emit(DownloadProgress(0f, "Preparing download..."))
                
                val areaDir = File(offlineDir, sanitizeFilename(areaName))
                areaDir.mkdirs()
                
                // Step 1: Save boundary data (20%)
                emit(DownloadProgress(0.2f, "Saving boundary data..."))
                if (boundary != null) {
                    val boundaryFile = File(areaDir, "boundary.json")
                    boundaryFile.writeText(json.encodeToString(boundary))
                }
                
                // Step 2: Save trails data (60%)
                emit(DownloadProgress(0.6f, "Saving trail data..."))
                val trailsFile = File(areaDir, "trails.json")
                trailsFile.writeText(json.encodeToString(trails))
                
                // Step 3: Save combined stats (80%)
                emit(DownloadProgress(0.8f, "Saving statistics..."))
                val statsFile = File(areaDir, "stats.json")
                statsFile.writeText(json.encodeToString(combinedStats))
                
                // Step 4: Save metadata (100%)
                emit(DownloadProgress(1.0f, "Finalizing..."))
                val metadata = OfflineAreaMetadata(
                    name = areaName,
                    downloadDate = System.currentTimeMillis(),
                    trailCount = trails.size,
                    totalDistanceKm = combinedStats.totalDistance / 1000.0,
                    totalElevationM = combinedStats.totalElevation,
                    fileSize = calculateDirSize(areaDir)
                )
                val metadataFile = File(areaDir, "metadata.json")
                metadataFile.writeText(json.encodeToString(metadata))
                
                emit(DownloadProgress(1.0f, "Download complete!"))
                
            } catch (e: Exception) {
                throw Exception("Failed to save offline area: ${e.message}")
            }
        }
    }
    
    /**
     * Get list of downloaded offline areas
     */
    suspend fun getOfflineAreas(): List<OfflineAreaMetadata> = withContext(Dispatchers.IO) {
        try {
            offlineDir.listFiles()?.mapNotNull { areaDir ->
                if (areaDir.isDirectory) {
                    val metadataFile = File(areaDir, "metadata.json")
                    if (metadataFile.exists()) {
                        try {
                            json.decodeFromString<OfflineAreaMetadata>(metadataFile.readText())
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Delete an offline area
     */
    suspend fun deleteOfflineArea(areaName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val areaDir = File(offlineDir, sanitizeFilename(areaName))
            areaDir.deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Load trails from offline storage
     */
    suspend fun loadOfflineTrails(areaName: String): List<OsmTrail>? = withContext(Dispatchers.IO) {
        try {
            val areaDir = File(offlineDir, sanitizeFilename(areaName))
            val trailsFile = File(areaDir, "trails.json")
            if (trailsFile.exists()) {
                json.decodeFromString<List<OsmTrail>>(trailsFile.readText())
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Load boundary from offline storage
     */
    suspend fun loadOfflineBoundary(areaName: String): OsmBoundary? = withContext(Dispatchers.IO) {
        try {
            val areaDir = File(offlineDir, sanitizeFilename(areaName))
            val boundaryFile = File(areaDir, "boundary.json")
            if (boundaryFile.exists()) {
                json.decodeFromString<OsmBoundary>(boundaryFile.readText())
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Load combined stats from offline storage
     */
    suspend fun loadOfflineStats(areaName: String): CombinedTrailStats? = withContext(Dispatchers.IO) {
        try {
            val areaDir = File(offlineDir, sanitizeFilename(areaName))
            val statsFile = File(areaDir, "stats.json")
            if (statsFile.exists()) {
                json.decodeFromString<CombinedTrailStats>(statsFile.readText())
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
    
    private fun calculateDirSize(dir: File): Long {
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
}

/**
 * Progress data for download operations
 */
data class DownloadProgress(
    val progress: Float, // 0.0 to 1.0
    val message: String
)

/**
 * Metadata for offline areas
 */
@kotlinx.serialization.Serializable
data class OfflineAreaMetadata(
    val name: String,
    val downloadDate: Long,
    val trailCount: Int,
    val totalDistanceKm: Double,
    val totalElevationM: Double,
    val fileSize: Long
)