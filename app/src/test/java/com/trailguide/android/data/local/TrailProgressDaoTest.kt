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
 * Unit tests for TrailProgressDao using in-memory Room database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TrailProgressDaoTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: TrailDatabase
    private lateinit var progressDao: TrailProgressDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrailDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        progressDao = database.trailProgressDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `insert and retrieve progress`() = runTest {
        val progress = TrailProgressEntity(
            id = "prog1",
            userId = "user1",
            trailId = "trail1",
            progressPercent = 50,
            distanceCoveredKm = 5.0,
            startedAt = System.currentTimeMillis()
        )
        
        progressDao.insertProgress(progress)
        
        val retrieved = progressDao.getProgressById("prog1")
        assertNotNull(retrieved)
        assertEquals(50, retrieved?.progressPercent)
        assertEquals(5.0, retrieved?.distanceCoveredKm, 0.01)
    }
    
    @Test
    fun `get progress by user and trail`() = runTest {
        val progress = TrailProgressEntity(
            id = "prog1",
            userId = "user1",
            trailId = "trail1",
            progressPercent = 75,
            distanceCoveredKm = 7.5,
            startedAt = System.currentTimeMillis()
        )
        
        progressDao.insertProgress(progress)
        
        val retrieved = progressDao.getProgressByUserAndTrail("user1", "trail1")
        assertNotNull(retrieved)
        assertEquals(75, retrieved?.progressPercent)
    }
    
    @Test
    fun `get completed trails`() = runTest {
        val completedProgress = TrailProgressEntity(
            id = "prog1",
            userId = "user1",
            trailId = "trail1",
            progressPercent = 100,
            distanceCoveredKm = 10.0,
            startedAt = System.currentTimeMillis(),
            completedAt = System.currentTimeMillis()
        )
        
        val inProgressTrail = TrailProgressEntity(
            id = "prog2",
            userId = "user1",
            trailId = "trail2",
            progressPercent = 50,
            distanceCoveredKm = 5.0,
            startedAt = System.currentTimeMillis(),
            completedAt = null
        )
        
        progressDao.insertProgressList(listOf(completedProgress, inProgressTrail))
        
        val completed = progressDao.getCompletedTrailsByUserId("user1").first()
        assertEquals(1, completed.size)
        assertEquals("prog1", completed[0].id)
    }
    
    @Test
    fun `get in-progress trails`() = runTest {
        val completedProgress = TrailProgressEntity(
            id = "prog1",
            userId = "user1",
            trailId = "trail1",
            progressPercent = 100,
            distanceCoveredKm = 10.0,
            startedAt = System.currentTimeMillis(),
            completedAt = System.currentTimeMillis()
        )
        
        val inProgressTrail = TrailProgressEntity(
            id = "prog2",
            userId = "user1",
            trailId = "trail2",
            progressPercent = 50,
            distanceCoveredKm = 5.0,
            startedAt = System.currentTimeMillis(),
            completedAt = null
        )
        
        progressDao.insertProgressList(listOf(completedProgress, inProgressTrail))
        
        val inProgress = progressDao.getInProgressTrailsByUserId("user1").first()
        assertEquals(1, inProgress.size)
        assertEquals("prog2", inProgress[0].id)
    }
    
    @Test
    fun `update progress`() = runTest {
        val progress = TrailProgressEntity(
            id = "prog1",
            userId = "user1",
            trailId = "trail1",
            progressPercent = 25,
            distanceCoveredKm = 2.5,
            startedAt = System.currentTimeMillis()
        )
        
        progressDao.insertProgress(progress)
        
        val updated = progress.copy(
            progressPercent = 75,
            distanceCoveredKm = 7.5
        )
        progressDao.updateProgress(updated)
        
        val retrieved = progressDao.getProgressById("prog1")
        assertEquals(75, retrieved?.progressPercent)
        assertEquals(7.5, retrieved?.distanceCoveredKm, 0.01)
    }
    
    @Test
    fun `get completed trail count`() = runTest {
        val progresses = listOf(
            TrailProgressEntity("prog1", "user1", "trail1", 100, 10.0, System.currentTimeMillis(), completedAt = System.currentTimeMillis()),
            TrailProgressEntity("prog2", "user1", "trail2", 100, 8.0, System.currentTimeMillis(), completedAt = System.currentTimeMillis()),
            TrailProgressEntity("prog3", "user1", "trail3", 50, 5.0, System.currentTimeMillis(), completedAt = null)
        )
        
        progressDao.insertProgressList(progresses)
        
        val count = progressDao.getCompletedTrailCount("user1")
        assertEquals(2, count)
    }
}

