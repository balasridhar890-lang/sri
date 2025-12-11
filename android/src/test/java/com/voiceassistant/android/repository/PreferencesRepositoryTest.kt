package com.voiceassistant.android.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.network.BackendClient
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
 * Unit tests for PreferencesRepository
 */
@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var appPreferences: AppPreferences

    @Mock
    private lateinit var backendClient: BackendClient

    private lateinit var repository: PreferencesRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        repository = PreferencesRepository(
            appPreferences = appPreferences,
            backendClient = backendClient
        )
    }

    @Test
    fun `getAllPreferences returns default preferences when no user ID`() = testScope.runTest {
        // Mock no user configured
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(-1))

        val preferences = repository.getAllPreferences().first()
        
        assertEquals(-1, preferences.userId)
        assertEquals("http://localhost:8000", preferences.backendUrl)
        assertTrue(preferences.autoAnswerEnabled)
        assertTrue(preferences.autoReplyEnabled)
    }

    @Test
    fun `getAllPreferences returns configured preferences`() = testScope.runTest {
        // Mock configured user
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        `when`(appPreferences.backendUrl).thenReturn(kotlinx.coroutines.flow.flowOf("http://example.com"))
        `when`(appPreferences.autoAnswerEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(false))
        `when`(appPreferences.autoReplyEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))
        `when`(appPreferences.voiceActivationEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))
        `when`(appPreferences.syncEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))
        `when`(appPreferences.darkModeEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))
        `when`(appPreferences.accessibilityEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(false))
        `when`(appPreferences.wakeWordSensitivity).thenReturn(kotlinx.coroutines.flow.flowOf(0.7f))
        `when`(appPreferences.voiceResponseVolume).thenReturn(kotlinx.coroutines.flow.flowOf(0.8f))
        `when`(appPreferences.voiceLanguage).thenReturn(kotlinx.coroutines.flow.flowOf("en-GB"))
        `when`(appPreferences.voiceGender).thenReturn(kotlinx.coroutines.flow.flowOf("female"))
        `when`(appPreferences.callMonitoringEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))
        `when`(appPreferences.smsMonitoringEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(false))
        `when`(appPreferences.contactSyncEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))
        `when`(appPreferences.batteryOptimizationDisabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))
        `when`(appPreferences.debugModeEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))
        `when`(appPreferences.logLevel).thenReturn(kotlinx.coroutines.flow.flowOf("DEBUG"))
        `when`(appPreferences.performanceMonitoringEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))

        val preferences = repository.getAllPreferences().first()
        
        assertEquals(1, preferences.userId)
        assertEquals("http://example.com", preferences.backendUrl)
        assertFalse(preferences.autoAnswerEnabled)
        assertTrue(preferences.autoReplyEnabled)
        assertTrue(preferences.darkModeEnabled)
        assertEquals(0.7f, preferences.wakeWordSensitivity)
        assertEquals("en-GB", preferences.voiceLanguage)
        assertEquals("female", preferences.voiceGender)
        assertTrue(preferences.debugModeEnabled)
        assertEquals("DEBUG", preferences.logLevel)
    }

    @Test
    fun `updatePreference marks for sync when user configured and sync enabled`() = testScope.runTest {
        // Mock configured user with sync enabled
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        `when`(appPreferences.syncEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))

        repository.updatePreference("autoAnswerEnabled", false)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(appPreferences).updateAutoAnswerEnabled(false)
        assertTrue(repository.hasPendingChanges())
        assertEquals(1, repository.getPendingChangesCount())
    }

    @Test
    fun `updatePreference does not mark for sync when no user configured`() = testScope.runTest {
        // Mock no user configured
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(-1))

        repository.updatePreference("autoAnswerEnabled", false)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(appPreferences).updateAutoAnswerEnabled(false)
        assertFalse(repository.hasPendingChanges())
    }

    @Test
    fun `updatePreference does not mark for sync when sync disabled`() = testScope.runTest {
        // Mock configured user but sync disabled
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        `when`(appPreferences.syncEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(false))

        repository.updatePreference("autoAnswerEnabled", false)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(appPreferences).updateAutoAnswerEnabled(false)
        assertFalse(repository.hasPendingChanges())
    }

    @Test
    fun `updatePreferences updates multiple settings`() = testScope.runTest {
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        `when`(appPreferences.syncEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))

        val settings = mapOf(
            "autoAnswerEnabled" to false,
            "wakeWordSensitivity" to 0.6f,
            "voiceLanguage" to "fr-FR"
        )
        
        repository.updatePreferences(settings)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(appPreferences).updateAutoAnswerEnabled(false)
        verify(appPreferences).updateWakeWordSensitivity(0.6f)
        verify(appPreferences).updateVoiceLanguage("fr-FR")
        assertEquals(3, repository.getPendingChangesCount())
    }

    @Test
    fun `syncWithBackend succeeds`() = testScope.runTest {
        // Mock configured user with pending changes
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        
        // Add a pending change
        repository.updatePreference("autoAnswerEnabled", false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Mock successful sync response
        val syncRequest = PreferencesSyncRequest(
            userId = 1,
            preferences = mapOf("autoAnswerEnabled" to false)
        )
        `when`(backendClient.syncPreferences(syncRequest)).thenReturn(
            PreferencesSyncResponse(success = true)
        )

        val success = repository.syncWithBackend(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        verify(backendClient).syncPreferences(syncRequest)
        assertFalse(repository.hasPendingChanges())
        assertEquals(0, repository.getPendingChangesCount())
    }

    @Test
    fun `syncWithBackend fails when backend returns error`() = testScope.runTest {
        // Mock configured user with pending changes
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        
        // Add a pending change
        repository.updatePreference("autoAnswerEnabled", false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Mock failed sync response
        val syncRequest = PreferencesSyncRequest(
            userId = 1,
            preferences = mapOf("autoAnswerEnabled" to false)
        )
        `when`(backendClient.syncPreferences(syncRequest)).thenReturn(
            PreferencesSyncResponse(success = false, message = "Invalid request")
        )

        val success = repository.syncWithBackend(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        verify(backendClient).syncPreferences(syncRequest)
        assertTrue(repository.hasPendingChanges()) // Changes should still be pending
        assertEquals(1, repository.getPendingChangesCount())
    }

    @Test
    fun `syncWithBackend handles network error`() = testScope.runTest {
        // Mock configured user with pending changes
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        
        // Add a pending change
        repository.updatePreference("autoAnswerEnabled", false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Mock network error
        val syncRequest = PreferencesSyncRequest(
            userId = 1,
            preferences = mapOf("autoAnswerEnabled" to false)
        )
        `when`(backendClient.syncPreferences(syncRequest)).thenThrow(
            RuntimeException("Network error")
        )

        val success = repository.syncWithBackend(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertTrue(repository.hasPendingChanges()) // Changes should still be pending
        assertEquals(1, repository.getPendingChangesCount())
    }

    @Test
    fun `syncWithBackend succeeds with no pending changes`() = testScope.runTest {
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))

        val success = repository.syncWithBackend(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        // Backend should not be called when there are no changes
        verify(backendClient, never()).syncPreferences(any())
    }

    @Test
    fun `forceSync updates local preferences`() = testScope.runTest {
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        
        // Mock backend preferences
        val backendPreferences = mapOf(
            "autoAnswerEnabled" to false,
            "wakeWordSensitivity" to 0.8f,
            "voiceLanguage" to "de-DE"
        )
        `when`(backendClient.getPreferences(1)).thenReturn(backendPreferences)

        val success = repository.forceSync(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        verify(backendClient).getPreferences(1)
        
        // Verify that local preferences were updated
        verify(appPreferences).updateAutoAnswerEnabled(false)
        verify(appPreferences).updateWakeWordSensitivity(0.8f)
        verify(appPreferences).updateVoiceLanguage("de-DE")
        
        // Clear pending changes after successful sync
        assertFalse(repository.hasPendingChanges())
    }

    @Test
    fun `forceSync handles backend error`() = testScope.runTest {
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        
        // Mock backend error
        `when`(backendClient.getPreferences(1)).thenThrow(
            RuntimeException("Backend unavailable")
        )

        val success = repository.forceSync(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        verify(backendClient).getPreferences(1)
        
        // No local preferences should be updated
        verify(appPreferences, never()).updateAutoAnswerEnabled(any())
        verify(appPreferences, never()).updateWakeWordSensitivity(any())
    }

    @Test
    fun `clearPendingChanges removes all pending changes`() = testScope.runTest {
        // Add some pending changes
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        `when`(appPreferences.syncEnabled).thenReturn(kotlinx.coroutines.flow.flowOf(true))
        
        repository.updatePreference("autoAnswerEnabled", false)
        repository.updatePreference("wakeWordSensitivity", 0.6f)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.hasPendingChanges())
        assertEquals(2, repository.getPendingChangesCount())

        // Clear all changes
        repository.clearPendingChanges()

        assertFalse(repository.hasPendingChanges())
        assertEquals(0, repository.getPendingChangesCount())
    }

    @Test
    fun `syncMutex prevents concurrent syncs`() = testScope.runTest {
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        repository.updatePreference("autoAnswerEnabled", false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Mock slow backend
        val syncRequest = PreferencesSyncRequest(
            userId = 1,
            preferences = mapOf("autoAnswerEnabled" to false)
        )
        `when`(backendClient.syncPreferences(syncRequest)).thenReturn(
            PreferencesSyncResponse(success = true)
        )

        // Start multiple sync operations concurrently
        val sync1 = repository.syncWithBackend(1)
        val sync2 = repository.syncWithBackend(1)
        
        testDispatcher.scheduler.advanceUntilIdle()

        // Both should succeed, but backend should only be called once due to mutex
        assertTrue(sync1)
        assertTrue(sync2)
        verify(backendClient, times(1)).syncPreferences(syncRequest)
    }

    @Test
    fun `updatePreference handles unknown keys gracefully`() = testScope.runTest {
        `when`(appPreferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))

        // This should not crash but also not update any preference
        repository.updatePreference("unknownKey", "someValue")
        testDispatcher.scheduler.advanceUntilIdle()

        // No preferences should be updated
        verify(appPreferences, never()).updateAutoAnswerEnabled(any())
        // But it should still be marked as pending change
        assertTrue(repository.hasPendingChanges())
    }
}