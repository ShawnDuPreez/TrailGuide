package com.trailguide.android.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for FavoriteTrailDao using in-memory Room database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FavoriteTrailDaoTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: TrailDatabase
    private lateinit var favoriteDao: FavoriteTrailDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrailDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        favoriteDao = database.favoriteTrailDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `insert and retrieve favorite`() = runTest {
        val favorite = FavoriteTrailEntity(
            id = "fav1",
            userId = "user1",
            trailId = "trail1",
            trailName = "Mountain Trail",
            trailImageUrl = null,
            syncStatus = SyncStatus.PENDING.name
        )
        
        favoriteDao.insertFavorite(favorite)
        
        val retrieved = favoriteDao.getFavoriteById("fav1")
        assertNotNull(retrieved)
        assertEquals("Mountain Trail", retrieved?.trailName)
    }
    
    @Test
    fun `check if trail is favorite`() = runTest {
        val favorite = FavoriteTrailEntity(
            id = "fav1",
            userId = "user1",
            trailId = "trail1",
            trailName = "Mountain Trail",
            trailImageUrl = null
        )
        
        favoriteDao.insertFavorite(favorite)
        
        val isFavorite = favoriteDao.isFavorite("user1", "trail1")
        assertTrue(isFavorite)
        
        val isNotFavorite = favoriteDao.isFavorite("user1", "trail2")
        assertFalse(isNotFavorite)
    }
    
    @Test
    fun `get favorites by sync status`() = runTest {
        val favorite1 = FavoriteTrailEntity(
            id = "fav1",
            userId = "user1",
            trailId = "trail1",
            trailName = "Trail 1",
            trailImageUrl = null,
            syncStatus = SyncStatus.PENDING.name
        )
        
        val favorite2 = FavoriteTrailEntity(
            id = "fav2",
            userId = "user1",
            trailId = "trail2",
            trailName = "Trail 2",
            trailImageUrl = null,
            syncStatus = SyncStatus.SYNCED.name
        )
        
        favoriteDao.insertFavorites(listOf(favorite1, favorite2))
        
        val pendingFavorites = favoriteDao.getFavoritesByStatus(SyncStatus.PENDING.name)
        assertEquals(1, pendingFavorites.size)
        assertEquals("fav1", pendingFavorites[0].id)
    }
    
    @Test
    fun `delete favorite by user and trail`() = runTest {
        val favorite = FavoriteTrailEntity(
            id = "fav1",
            userId = "user1",
            trailId = "trail1",
            trailName = "Mountain Trail",
            trailImageUrl = null
        )
        
        favoriteDao.insertFavorite(favorite)
        assertTrue(favoriteDao.isFavorite("user1", "trail1"))
        
        favoriteDao.deleteFavoriteByUserAndTrail("user1", "trail1")
        assertFalse(favoriteDao.isFavorite("user1", "trail1"))
    }
    
    @Test
    fun `get favorites count`() = runTest {
        val favorites = listOf(
            FavoriteTrailEntity("fav1", "user1", "trail1", "Trail 1", null),
            FavoriteTrailEntity("fav2", "user1", "trail2", "Trail 2", null),
            FavoriteTrailEntity("fav3", "user2", "trail3", "Trail 3", null)
        )
        
        favoriteDao.insertFavorites(favorites)
        
        val user1Count = favoriteDao.getFavoriteCount("user1")
        assertEquals(2, user1Count)
        
        val user2Count = favoriteDao.getFavoriteCount("user2")
        assertEquals(1, user2Count)
    }
}

