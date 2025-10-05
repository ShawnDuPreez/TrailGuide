package com.trailguide.android.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for trail collections
 */
@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>
    
    @Query("SELECT * FROM collections WHERE id = :collectionId")
    fun getCollectionById(collectionId: String): Flow<CollectionEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)
    
    @Update
    suspend fun updateCollection(collection: CollectionEntity)
    
    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)
    
    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollectionById(collectionId: String)
}

