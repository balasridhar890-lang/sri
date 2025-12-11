package com.voiceassistant.android.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.repository.PreferencesRepository
import com.voiceassistant.android.repository.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.Mockito.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SettingsViewModel
 */
@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var preferences: AppPreferences

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    @Mock
    private lateinit var context: Context

    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        viewModel = SettingsViewModel(
            preferences = preferences,
            preferencesRepository = preferencesRepository,
            context = context
        )
    }

    @Test
    fun `initial state is loading`() = testScope.runTest {
        val state = viewModel.uiState.first()
        assertTrue(state.isLoading)
    }

    @Test
    fun `updateSetting updates preference and state`() = testScope.runTest {
        // Mock the repository response
        val userPreferences = UserPreferences(
            userId = 1,
            autoAnswerEnabled = true
        )
        `when`(preferencesRepository.getAllPreferences()).thenReturn(kotlinx.coroutines.flow.flowOf(userPreferences))
        `when`(preferencesRepository.hasPendingChanges()).thenReturn(false)
        `when`(preferencesRepository.getPendingChangesCount()).thenReturn(0)

        viewModel.updateSetting("autoAnswerEnabled", false)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesRepository).updatePreference("autoAnswerEnabled", false)

        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `updateApiConfig updates API settings`() = testScope.runTest {
        // Mock the repository response
        val userPreferences = UserPreferences(
            userId = 1,
            backendUrl = "http://localhost:8000"
        )
        `when`(preferencesRepository.getAllPreferences()).thenReturn(kotlinx.coroutines.flow.flowOf(userPreferences))
        `when`(preferencesRepository.hasPendingChanges()).thenReturn(false)

        viewModel.updateApiConfig("http://example.com", 123)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesRepository).updatePreferences(
            mapOf(
                "backendUrl" to "http://example.com",
                "userId" to 123
            )
        )
    }

    @Test
    fun `updateApiCredentials updates API credentials`() = testScope.runTest {
        viewModel.updateApiCredentials("test-api-key", "test-token")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferences).updateApiKey("test-api-key")
        verify(preferences).updateApiToken("test-token")
    }

    @Test
    fun `updateVoiceSettings updates voice preferences`() = testScope.runTest {
        viewModel.updateVoiceSettings(0.7f, 0.8f, "en-US", "female")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesRepository).updatePreferences(
            mapOf(
                "wakeWordSensitivity" to 0.7f,
                "voiceResponseVolume" to 0.8f,
                "voiceLanguage" to "en-US",
                "voiceGender" to "female"
            )
        )
    }

    @Test
    fun `updateFeatureToggles updates feature settings`() = testScope.runTest {
        viewModel.updateFeatureToggles(
            autoAnswer = true,
            autoReply = false,
            voiceActivation = true,
            callMonitoring = true,
            smsMonitoring = false,
            contactSync = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesRepository).updatePreferences(
            mapOf(
                "autoAnswerEnabled" to true,
                "autoReplyEnabled" to false,
                "voiceActivationEnabled" to true,
                "callMonitoringEnabled" to true,
                "smsMonitoringEnabled" to false,
                "contactSyncEnabled" to true
            )
        )
    }

    @Test
    fun `syncWithBackend succeeds`() = testScope.runTest {
        // Mock successful sync
        `when`(preferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        `when`(preferencesRepository.syncWithBackend(1)).thenReturn(true)
        `when`(preferences.lastBackupTimestamp).thenReturn(kotlinx.coroutines.flow.flowOf(System.currentTimeMillis()))

        viewModel.syncWithBackend()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isSyncing)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `syncWithBackend fails with no user ID`() = testScope.runTest {
        `when`(preferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(-1))

        viewModel.syncWithBackend()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isSyncing)
        assertTrue(state.syncError?.contains("User ID not configured") == true)
    }

    @Test
    fun `syncWithBackend fails with network error`() = testScope.runTest {
        // Mock failed sync
        `when`(preferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        `when`(preferencesRepository.syncWithBackend(1)).thenReturn(false)

        viewModel.syncWithBackend()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isSyncing)
        assertTrue(state.syncError?.contains("Sync failed") == true)
    }

    @Test
    fun `forceSyncFromBackend succeeds`() = testScope.runTest {
        // Mock successful force sync
        `when`(preferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        `when`(preferencesRepository.forceSync(1)).thenReturn(true)

        viewModel.forceSyncFromBackend()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesRepository).forceSync(1)
    }

    @Test
    fun `resetToDefaults clears all settings`() = testScope.runTest {
        viewModel.resetToDefaults()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferences).resetAll()
        verify(preferencesRepository).clearPendingChanges()
    }

    @Test
    fun `exportSettings returns JSON string`() = testScope.runTest {
        // Mock preferences data
        val userPreferences = UserPreferences(
            userId = 1,
            backendUrl = "http://localhost:8000",
            autoAnswerEnabled = true,
            autoReplyEnabled = false
        )
        `when`(preferencesRepository.getAllPreferences()).thenReturn(kotlinx.coroutines.flow.flowOf(userPreferences))

        val exported = viewModel.exportSettings()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(exported.contains("\"backendUrl\""))
        assertTrue(exported.contains("\"autoAnswerEnabled\":true"))
        assertTrue(exported.contains("\"autoReplyEnabled\":false"))
    }

    @Test
    fun `hasPendingChanges returns correct value`() = testScope.runTest {
        `when`(preferencesRepository.hasPendingChanges()).thenReturn(true)
        `when`(preferencesRepository.getPendingChangesCount()).thenReturn(3)

        assertTrue(viewModel.hasPendingChanges())
        assertEquals(3, viewModel.getPendingChangesCount())
    }

    @Test
    fun `clearErrors removes error messages`() = testScope.runTest {
        // First trigger an error
        `when`(preferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(-1))
        viewModel.syncWithBackend()
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.first()
        assertTrue(state.syncError != null)

        // Clear errors
        viewModel.clearErrors()
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.first()
        assertTrue(state.syncError == null)
        assertTrue(state.saveError == null)
    }

    @Test
    fun `updateUiPreferences updates UI settings`() = testScope.runTest {
        viewModel.updateUiPreferences(
            darkMode = true,
            accessibility = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesRepository).updatePreferences(
            mapOf(
                "darkModeEnabled" to true,
                "accessibilityEnabled" to true
            )
        )
    }

    @Test
    fun `updateDebugSettings updates debug preferences`() = testScope.runTest {
        viewModel.updateDebugSettings(
            debugMode = true,
            logLevel = "DEBUG",
            performanceMonitoring = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        verify(preferencesRepository).updatePreferences(
            mapOf(
                "debugModeEnabled" to true,
                "logLevel" to "DEBUG",
                "performanceMonitoringEnabled" to true
            )
        )
    }
}