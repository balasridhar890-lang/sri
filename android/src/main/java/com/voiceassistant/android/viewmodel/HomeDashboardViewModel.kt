package com.voiceassistant.android.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.android.database.CallLogDao
import com.voiceassistant.android.database.SMSLogDao
import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI state for home dashboard
 */
data class HomeDashboardState(
    val isLoading: Boolean = true,
    val voiceStatus: VoiceStatus = VoiceStatus.IDLE,
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val runningApps: List<RunningApp> = emptyList(),
    val recentCalls: List<CallLogItem> = emptyList(),
    val recentSMS: List<SMSLogItem> = emptyList(),
    val stats: AppStats = AppStats(),
    val error: String? = null
)

/**
 * Voice assistant status
 */
enum class VoiceStatus {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

/**
 * Battery information
 */
data class BatteryInfo(
    val level: Int = 0,
    val status: String = "Unknown",
    val isCharging: Boolean = false,
    val temperature: Float = 0.0f
)

/**
 * Running application info
 */
data class RunningApp(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.Bitmap? = null
)

/**
 * Call log item for display
 */
data class CallLogItem(
    val phoneNumber: String,
    val direction: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val success: Boolean
) {
    val formattedTime: String = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        .format(Date(timestamp))
    val formattedDuration: String = if (durationSeconds > 0) {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        "${minutes}m ${seconds}s"
    } else "0s"
}

/**
 * SMS log item for display
 */
data class SMSLogItem(
    val phoneNumber: String,
    val messageBody: String,
    val decision: String,
    val timestamp: Long,
    val replyText: String
) {
    val formattedTime: String = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        .format(Date(timestamp))
}

/**
 * Application statistics
 */
data class AppStats(
    val totalCalls: Int = 0,
    val totalSMS: Int = 0,
    val successfulCalls: Int = 0,
    val autoReplyRate: Float = 0.0f,
    val avgCallDuration: Long = 0L,
    val lastBackup: Long = 0L
)

/**
 * ViewModel for home dashboard
 */
@Singleton
class HomeDashboardViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val preferencesRepository: PreferencesRepository,
    private val callLogDao: CallLogDao,
    private val smsLogDao: SMSLogDao,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeDashboardState())
    val uiState: StateFlow<HomeDashboardState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    /**
     * Load all dashboard data
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Load all data in parallel
                val batteryInfo = getBatteryInfo()
                val runningApps = getRunningApps()
                val recentCalls = getRecentCalls()
                val recentSMS = getRecentSMS()
                val stats = getAppStats()
                val voiceStatus = getVoiceStatus()
                
                _uiState.value = HomeDashboardState(
                    isLoading = false,
                    voiceStatus = voiceStatus,
                    batteryInfo = batteryInfo,
                    runningApps = runningApps,
                    recentCalls = recentCalls,
                    recentSMS = recentSMS,
                    stats = stats
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard data"
                )
            }
        }
    }
    
    /**
     * Get current battery information
     */
    private fun getBatteryInfo(): BatteryInfo {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val isCharging = batteryManager.isCharging
        
        val statusString = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            BatteryManager.BATTERY_STATUS_UNKNOWN -> "Unknown"
            else -> "Unknown"
        }
        
        // Get temperature if available
        val temperatureIntent = context.registerReceiver(null, 
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temperature = temperatureIntent?.let { intent ->
            val temp = intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)
            temp / 10.0f // Convert from tenths of a degree Celsius
        } ?: 0.0f
        
        return BatteryInfo(
            level = level,
            status = statusString,
            isCharging = isCharging,
            temperature = temperature
        )
    }
    
    /**
     * Get list of running applications
     */
    private fun getRunningApps(): List<RunningApp> {
        val packageManager = context.packageManager
        val runningApps = mutableListOf<RunningApp>()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) 
                    as android.app.AppOpsManager
                val uid = android.os.Process.myUid()
                val pkgName = context.packageName
                
                val mode = appOpsManager.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_RUN_IN_BACKGROUND,
                    uid,
                    pkgName
                )
                
                // Get running processes (simplified for this example)
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                
                installedApps.filter { app ->
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 // Filter out system apps
                }.take(10).forEach { app ->
                    val appName = packageManager.getApplicationLabel(app).toString()
                    runningApps.add(
                        RunningApp(
                            packageName = app.packageName,
                            appName = appName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback to empty list if we can't get running apps
        }
        
        return runningApps
    }
    
    /**
     * Get recent call logs
     */
    private suspend fun getRecentCalls(): List<CallLogItem> {
        return try {
            callLogDao.getRecentLogs(5).map { entity ->
                CallLogItem(
                    phoneNumber = entity.phoneNumber,
                    direction = entity.direction,
                    timestamp = entity.timestamp,
                    durationSeconds = entity.durationSeconds,
                    success = entity.success
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get recent SMS logs
     */
    private suspend fun getRecentSMS(): List<SMSLogItem> {
        return try {
            smsLogDao.getRecentLogs(5).map { entity ->
                SMSLogItem(
                    phoneNumber = entity.phoneNumber,
                    messageBody = entity.messageBody.take(50) + if (entity.messageBody.length > 50) "..." else "",
                    decision = entity.decision,
                    timestamp = entity.timestamp,
                    replyText = entity.replyText
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get application statistics
     */
    private suspend fun getAppStats(): AppStats {
        return try {
            val totalCalls = preferences.totalCallsLogged.first()
            val totalSMS = preferences.totalSmsLogged.first()
            val lastBackup = preferences.lastBackupTimestamp.first()
            
            // Calculate additional stats from database
            val recentCalls = callLogDao.getRecentLogs(100)
            val successfulCalls = recentCalls.count { it.success }
            val avgCallDuration = if (recentCalls.isNotEmpty()) {
                recentCalls.sumOf { it.durationSeconds } / recentCalls.size
            } else 0L
            
            val recentSMS = smsLogDao.getRecentLogs(100)
            val autoReplyRate = if (recentSMS.isNotEmpty()) {
                recentSMS.count { it.decision == "yes" }.toFloat() / recentSMS.size
            } else 0.0f
            
            AppStats(
                totalCalls = totalCalls,
                totalSMS = totalSMS,
                successfulCalls = successfulCalls,
                autoReplyRate = autoReplyRate,
                avgCallDuration = avgCallDuration,
                lastBackup = lastBackup
            )
        } catch (e: Exception) {
            AppStats()
        }
    }
    
    /**
     * Get current voice assistant status
     */
    private fun getVoiceStatus(): VoiceStatus {
        // This would be connected to actual voice assistant service
        // For now, return IDLE as placeholder
        return VoiceStatus.IDLE
    }
    
    /**
     * Refresh dashboard data
     */
    fun refreshDashboard() {
        loadDashboardData()
    }
    
    /**
     * Force sync with backend
     */
    fun forceSync() {
        viewModelScope.launch {
            try {
                val userId = preferences.userId.first()
                if (userId != -1) {
                    val success = preferencesRepository.forceSync(userId)
                    if (!success) {
                        _uiState.value = _uiState.value.copy(
                            error = "Sync failed. Check your internet connection."
                        )
                    } else {
                        loadDashboardData() // Refresh after successful sync
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Sync failed: ${e.message}"
                )
            }
        }
    }
}