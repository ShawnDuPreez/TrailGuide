package com.trailguide.android.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.trailguide.android.data.datastore.SecureSessionStore
import com.trailguide.android.data.datastore.UserPreferences
import com.trailguide.android.data.repository.AuthRepository
import com.trailguide.android.data.security.BiometricAuthenticationManager
import com.trailguide.android.data.sync.SyncScheduler
import com.trailguide.android.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SettingsViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var userPreferences: UserPreferences
    
    @Mock
    private lateinit var secureSessionStore: SecureSessionStore
    
    @Mock
    private lateinit var authRepository: AuthRepository
    
    @Mock
    private lateinit var biometricAuthManager: BiometricAuthenticationManager
    
    @Mock
    private lateinit var syncScheduler: SyncScheduler
    
    private lateinit var viewModel: SettingsViewModel
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock behaviors
        whenever(userPreferences.languageFlow).thenReturn(flowOf(UserPreferences.LANGUAGE_ENGLISH))
        whenever(userPreferences.notificationsEnabledFlow).thenReturn(flowOf(true))
        whenever(userPreferences.biometricEnabledFlow).thenReturn(flowOf(false))
        whenever(userPreferences.weatherAlertsFlow).thenReturn(flowOf(true))
        whenever(userPreferences.friendActivityFlow).thenReturn(flowOf(true))
        whenever(userPreferences.newTrailsFlow).thenReturn(flowOf(true))
        whenever(userPreferences.themeModeFlow).thenReturn(flowOf(UserPreferences.THEME_SYSTEM))
        whenever(biometricAuthManager.canUseBiometric()).thenReturn(true)
        
        viewModel = SettingsViewModel(
            userPreferences,
            secureSessionStore,
            authRepository,
            biometricAuthManager,
            syncScheduler
        )
    }
    
    @After
    fun teardown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `biometric availability is checked on init`() {
        assertTrue(viewModel.uiState.value.biometricAvailable)
        verify(biometricAuthManager).canUseBiometric()
    }
    
    @Test
    fun `set language calls user preferences`() = runTest {
        viewModel.setLanguage(UserPreferences.LANGUAGE_AFRIKAANS)
        advanceUntilIdle()
        
        verify(userPreferences).setLanguage(UserPreferences.LANGUAGE_AFRIKAANS)
    }
    
    @Test
    fun `set notifications enabled calls user preferences`() = runTest {
        viewModel.setNotificationsEnabled(false)
        advanceUntilIdle()
        
        verify(userPreferences).setNotificationsEnabled(false)
    }
    
    @Test
    fun `disable biometric clears credentials`() = runTest {
        viewModel.setBiometricEnabled(false)
        advanceUntilIdle()
        
        verify(userPreferences).setBiometricEnabled(false)
        verify(secureSessionStore).setBiometricEnabled(false)
        verify(authRepository).clearBiometricCredentials()
    }
    
    @Test
    fun `enable biometric does not clear credentials`() = runTest {
        viewModel.setBiometricEnabled(true)
        advanceUntilIdle()
        
        verify(userPreferences).setBiometricEnabled(true)
        verify(secureSessionStore).setBiometricEnabled(true)
        verify(authRepository, never()).clearBiometricCredentials()
    }
    
    @Test
    fun `trigger manual sync schedules one-time sync`() = runTest {
        viewModel.triggerManualSync()
        advanceUntilIdle()
        
        verify(syncScheduler).scheduleOneTimeSync()
    }
    
    @Test
    fun `get app version returns version string`() {
        val version = viewModel.getAppVersion()
        assertTrue(version.isNotEmpty())
    }
}

