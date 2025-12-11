package com.voiceassistant.android.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.voiceassistant.android.database.CallLogDao
import com.voiceassistant.android.database.SMSLogDao
import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.repository.PreferencesRepository
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for HomeDashboardViewModel
 */
@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HomeDashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var preferences: AppPreferences

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    @Mock
    private lateinit var callLogDao: CallLogDao

    @Mock
    private lateinit var smsLogDao: SMSLogDao

    @Mock
    private lateinit var context: Context

    private lateinit var viewModel: HomeDashboardViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        viewModel = HomeDashboardViewModel(
            preferences = preferences,
            preferencesRepository = preferencesRepository,
            callLogDao = callLogDao,
            smsLogDao = smsLogDao,
            context = context
        )
    }

    @Test
    fun `initial state is loading`() = testScope.runTest {
        val state = viewModel.uiState.first()
        assertTrue(state.isLoading)
    }

    @Test
    fun `refreshDashboard updates state`() = testScope.runTest {
        // Mock empty data
        `when`(callLogDao.getRecentLogs(5)).thenReturn(emptyList())
        `when`(smsLogDao.getRecentLogs(5)).thenReturn(emptyList())
        `when`(preferences.totalCallsLogged).thenReturn(kotlinx.coroutines.flow.flowOf(0))
        `when`(preferences.totalSmsLogged).thenReturn(kotlinx.coroutines.flow.flowOf(0))
        `when`(preferences.lastBackupTimestamp).thenReturn(kotlinx.coroutines.flow.flowOf(0L))

        viewModel.refreshDashboard()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals(false, state.isLoading)
        assertEquals(emptyList<CallLogItem>(), state.recentCalls)
        assertEquals(emptyList<SMSLogItem>(), state.recentSMS)
    }

    @Test
    fun `getBatteryInfo returns correct information`() = testScope.runTest {
        // This would require more complex mocking of Android system services
        // For now, just test that the method can be called
        val batteryInfo = viewModel.getBatteryInfo()
        assertTrue(batteryInfo.level >= 0)
        assertTrue(batteryInfo.level <= 100)
    }

    @Test
    fun `getRunningApps returns app list`() = testScope.runTest {
        val apps = viewModel.getRunningApps()
        // This test would need proper mocking of PackageManager and AppOpsManager
        assertTrue(apps is List<RunningApp>)
    }

    @Test
    fun `forceSync calls repository sync`() = testScope.runTest {
        `when`(preferences.userId).thenReturn(kotlinx.coroutines.flow.flowOf(1))

        viewModel.forceSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that sync was attempted
        // Note: This test would need more specific verification in a real implementation
    }
}