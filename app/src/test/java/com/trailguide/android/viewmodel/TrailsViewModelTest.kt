package com.trailguide.android.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.repository.GoogleTrailRepository
import com.trailguide.android.data.repository.TrailRepository
import com.trailguide.android.presentation.viewmodel.TrailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for TrailsViewModel.
 * Tests trail loading, filtering, and error handling.
 */
@ExperimentalCoroutinesApi
class TrailsViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var trailRepository: TrailRepository
    
    @Mock
    private lateinit var googleTrailRepository: GoogleTrailRepository
    
    private lateinit var viewModel: TrailsViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    
    private val mockTrails = listOf(
        Trail(
            id = "1",
            name = "Easy Trail",
            city = "Test City",
            latitude = 0.0,
            longitude = 0.0,
            distanceKm = 5.0,
            elevationM = 100,
            difficulty = Difficulty.EASY,
            rating = 4.5,
            imageUrl = null
        ),
        Trail(
            id = "2",
            name = "Hard Trail",
            city = "Test City",
            latitude = 0.0,
            longitude = 0.0,
            distanceKm = 15.0,
            elevationM = 500,
            difficulty = Difficulty.HARD,
            rating = 4.8,
            imageUrl = null
        )
    )
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        runBlocking {
            whenever(
                googleTrailRepository.searchHikingTrails(
                    any<Double>(),
                    any<Double>(),
                    any<Int>(),
                    any<String>()
                )
            ).thenReturn(flow { emit(NetworkResult.Success(mockTrails)) })
        }
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadTrails should update trails state on success`() = runTest {
        // Given
        val flow = flow {
            emit(NetworkResult.Loading)
            emit(NetworkResult.Success(mockTrails))
        }
        whenever(trailRepository.getAllTrails()).thenReturn(flow)
        
        // When
        viewModel = TrailsViewModel(trailRepository, googleTrailRepository)
        viewModel.useGoogleTrails(false)
        advanceUntilIdle()
        
        // Then
        assertEquals(mockTrails, viewModel.trails.value)
        assertFalse(viewModel.isLoading.value)
        assertEquals(null, viewModel.errorMessage.value)
    }
    
    @Test
    fun `loadTrails should update error state on failure`() = runTest {
        // Given
        val errorMessage = "Network error"
        val flow = flow {
            emit(NetworkResult.Loading)
            emit(NetworkResult.Error(errorMessage))
        }
        whenever(trailRepository.getAllTrails()).thenReturn(flow)
        
        // When
        viewModel = TrailsViewModel(trailRepository, googleTrailRepository)
        viewModel.useGoogleTrails(false)
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.trails.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
        assertEquals(errorMessage, viewModel.errorMessage.value)
    }
    
    @Test
    fun `setSearchQuery should filter trails by name`() = runTest {
        // Given
        val flow = flow { emit(NetworkResult.Success(mockTrails)) }
        whenever(trailRepository.getAllTrails()).thenReturn(flow)
        viewModel = TrailsViewModel(trailRepository, googleTrailRepository)
        viewModel.useGoogleTrails(false)
        advanceUntilIdle()
        advanceUntilIdle()
        assertEquals(mockTrails, viewModel.trails.value)
        
        // When
        viewModel.setSearchQuery("Easy")
        advanceUntilIdle()
        
        // Then
        assertEquals(1, viewModel.filteredTrails.value.size)
        assertEquals("Easy Trail", viewModel.filteredTrails.value[0].name)
    }
    
    @Test
    fun `setDifficulty should filter trails by difficulty`() = runTest {
        // Given
        val flow = flow { emit(NetworkResult.Success(mockTrails)) }
        whenever(trailRepository.getAllTrails()).thenReturn(flow)
        viewModel = TrailsViewModel(trailRepository, googleTrailRepository)
        viewModel.useGoogleTrails(false)
        advanceUntilIdle()
        advanceUntilIdle()
        
        // When
        viewModel.setDifficulty(Difficulty.EASY)
        advanceUntilIdle()
        
        // Then
        assertEquals(1, viewModel.filteredTrails.value.size)
        assertEquals(Difficulty.EASY, viewModel.filteredTrails.value[0].difficulty)
    }
    
    @Test
    fun `setMaxDistance should filter trails by distance`() = runTest {
        // Given
        val flow = flow { emit(NetworkResult.Success(mockTrails)) }
        whenever(trailRepository.getAllTrails()).thenReturn(flow)
        viewModel = TrailsViewModel(trailRepository, googleTrailRepository)
        viewModel.useGoogleTrails(false)
        advanceUntilIdle()
        advanceUntilIdle()
        
        // When
        viewModel.setMaxDistance(10.0)
        advanceUntilIdle()
        
        // Then
        assertEquals(1, viewModel.filteredTrails.value.size)
        assertEquals(5.0, viewModel.filteredTrails.value[0].distanceKm)
    }
    
    @Test
    fun `clearFilters should reset all filters`() = runTest {
        // Given
        val flow = flow { emit(NetworkResult.Success(mockTrails)) }
        whenever(trailRepository.getAllTrails()).thenReturn(flow)
        viewModel = TrailsViewModel(trailRepository, googleTrailRepository)
        viewModel.useGoogleTrails(false)
        viewModel.setSearchQuery("test")
        viewModel.setDifficulty(Difficulty.EASY)
        viewModel.setMaxDistance(5.0)
        advanceUntilIdle()
        
        // When
        viewModel.clearFilters()
        advanceUntilIdle()
        
        // Then
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(null, viewModel.selectedDifficulty.value)
        assertEquals(20.0, viewModel.maxDistance.value)
    }
}

