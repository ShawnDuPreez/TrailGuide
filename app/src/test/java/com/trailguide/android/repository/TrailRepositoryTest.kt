package com.trailguide.android.repository

import com.trailguide.android.data.dto.TrailDto
import com.trailguide.android.data.local.DownloadedTrailDao
import com.trailguide.android.data.local.FavoriteTrailDao
import com.trailguide.android.data.remote.NetworkResult
import com.trailguide.android.data.remote.TrailApiService
import com.trailguide.android.data.repository.SupabaseAuthProvider
import com.trailguide.android.data.repository.TrailRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for TrailRepository.
 * Tests API integration and data transformation.
 */
@ExperimentalCoroutinesApi
@RunWith(org.robolectric.RobolectricTestRunner::class)
class TrailRepositoryTest {
    
    @Mock
    private lateinit var apiService: TrailApiService
    
    @Mock
    private lateinit var downloadedTrailDao: DownloadedTrailDao
    
    @Mock
    private lateinit var favoriteTrailDao: FavoriteTrailDao
    
    @Mock
    private lateinit var supabaseClient: SupabaseClient
    
    @Mock
    private lateinit var supabaseAuthProvider: SupabaseAuthProvider
    
    private lateinit var repository: TrailRepository
    
    private val mockTrailDto = TrailDto(
        id = "1",
        name = "Test Trail",
        city = "Test City",
        latitude = -25.0,
        longitude = 28.0,
        distanceKm = 10.0,
        elevationM = 200,
        difficulty = "moderate",
        rating = 4.5,
        imageUrl = "https://example.com/image.jpg",
        tags = listOf("scenic", "family"),
        description = "A beautiful test trail"
    )
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(supabaseAuthProvider.currentUserId()).thenReturn(null)
        repository = TrailRepository(apiService, downloadedTrailDao, favoriteTrailDao, supabaseClient, supabaseAuthProvider)
    }
    
    @Test
    fun `getAllTrails should return success with trail list`() = runTest {
        // Given
        val response = Response.success(listOf(mockTrailDto))
        whenever(apiService.getAllTrails()).thenReturn(response)
        whenever(apiService.getFavoriteTrails(any<String>())).thenReturn(Response.success(emptyList()))
        
        // When
        val flow = repository.getAllTrails()
        val result = flow.drop(1).first()
        
        // Then
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
        assertEquals("Test Trail", result.data[0].name)
    }
    
    @Test
    fun `getAllTrails should return error on API failure`() = runTest {
        // Given
        val errorResponse = Response.error<List<TrailDto>>(
            404,
            okhttp3.ResponseBody.create(null, "Not found")
        )
        whenever(apiService.getAllTrails()).thenReturn(errorResponse)
        whenever(apiService.getFavoriteTrails(any<String>())).thenReturn(Response.success(emptyList()))
        
        // When
        val flow = repository.getAllTrails()
        val result = flow.drop(1).first()
        
        // Then
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("404"))
    }
    
    @Test
    fun `getTrailById should return correct trail`() = runTest {
        // Given
        val response = Response.success(mockTrailDto)
        whenever(apiService.getTrailById("1")).thenReturn(response)
        
        // When
        val flow = repository.getTrailById("1")
        val result = flow.drop(1).first()
        
        // Then
        assertTrue(result is NetworkResult.Success)
        assertEquals("1", (result as NetworkResult.Success).data.id)
        assertEquals("Test Trail", result.data.name)
    }
    
    @Test
    fun `searchTrails should apply filters correctly`() = runTest {
        // Given
        val response = Response.success(listOf(mockTrailDto))
        whenever(
            apiService.searchTrails(
                query = "test",
                difficulty = "moderate",
                maxDistance = 15.0
            )
        ).thenReturn(response)
        
        // When
        val flow = repository.searchTrails(
            query = "test",
            difficulty = com.trailguide.android.data.model.Difficulty.MODERATE,
            maxDistance = 15.0
        )
        val result = flow.drop(1).first()
        
        // Then
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
    }
}

