package com.voiceassistant.android.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Debug
import android.telephony.TelephonyManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.network.BackendClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI state for diagnostics screen
 */
data class DiagnosticsState(
    val isLoadingLogs: Boolean = false,
    val logs: List<LogEntry> = emptyList(),
    val selectedLogLevel: String = "ALL",
    val availableLogLevels: List<String> = listOf("ALL", "VERBOSE", "DEBUG", "INFO", "WARN", "ERROR"),
    val systemInfo: SystemInfo = SystemInfo(),
    val testResults: List<TestResult> = emptyList(),
    val error: String? = null
)

/**
 * Log entry for display
 */
data class LogEntry(
    val level: String,
    val tag: String,
    val message: String,
    val timestamp: String
)

/**
 * System information
 */
data class SystemInfo(
    val androidVersion: String = "",
    val sdkVersion: Int = 0,
    val deviceModel: String = "",
    val appVersion: String = "",
    val appVersionCode: String = "",
    val availableProcessors: Int = 0,
    val totalMemory: Long = 0L,
    val availableMemory: Long = 0L,
    val totalStorage: Long = 0L,
    val availableStorage: Long = 0L,
    val networkType: String = "",
    val networkState: String = "",
    val requestedPermissions: Int = 0,
    val grantedPermissions: Int = 0
)

/**
 * Test result for diagnostics
 */
data class TestResult(
    val testName: String,
    val success: Boolean? = null,
    val message: String = "",
    val timestamp: String = SimpleDateFormat("HH:mm:ss").format(Date())
)

/**
 * ViewModel for diagnostics screen
 */
@Singleton
class DiagnosticsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val backendClient: BackendClient,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DiagnosticsState())
    val uiState: StateFlow<DiagnosticsState> = _uiState.asStateFlow()
    
    init {
        loadSystemInfo()
        refreshLogs()
    }
    
    /**
     * Load system information
     */
    private fun loadSystemInfo() {
        viewModelScope.launch {
            try {
                val systemInfo = getSystemInformation()
                _uiState.value = _uiState.value.copy(systemInfo = systemInfo)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load system info: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get comprehensive system information
     */
    private fun getSystemInformation(): SystemInfo {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Memory information
        val runtime = Runtime.getRuntime()
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        
        // Storage information
        val internalDir = context.filesDir
        val stat = android.os.StatFs(internalDir.absolutePath)
        val totalStorage = stat.totalBytes
        val availableStorage = stat.availableBytes
        
        // Network information
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val networkType = when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Unknown"
        }
        
        val networkState = if (connectivityManager.activeNetworkInfo?.isConnected == true) {
            "Connected"
        } else {
            "Disconnected"
        }
        
        // Permission information
        val requestedPermissions = arrayOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.SEND_SMS
        )
        
        val grantedPermissions = requestedPermissions.count { permission ->
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
        
        return SystemInfo(
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = packageInfo.versionName,
            appVersionCode = Build.VERSION.SDK_INT.toString(),
            availableProcessors = Runtime.getRuntime().availableProcessors(),
            totalMemory = runtime.maxMemory(),
            availableMemory = runtime.totalMemory() - runtime.freeMemory(),
            totalStorage = totalStorage,
            availableStorage = availableStorage,
            networkType = networkType,
            networkState = networkState,
            requestedPermissions = requestedPermissions.size,
            grantedPermissions = grantedPermissions
        )
    }
    
    /**
     * Refresh logs from all sources
     */
    fun refreshLogs() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingLogs = true, error = null)
                
                val logs = getSystemLogs()
                val selectedLevel = _uiState.value.selectedLogLevel
                
                val filteredLogs = if (selectedLevel == "ALL") {
                    logs
                } else {
                    logs.filter { it.level == selectedLevel }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoadingLogs = false,
                    logs = filteredLogs
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingLogs = false,
                    error = "Failed to load logs: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get system logs (simplified implementation)
     */
    private fun getSystemLogs(): List<LogEntry> {
        // This is a simplified implementation
        // In a real app, you might want to use LogcatReader or maintain your own log buffer
        val logs = mutableListOf<LogEntry>()
        
        // Add some simulated log entries for demonstration
        val simulatedLogs = listOf(
            LogEntry("INFO", "VoiceAssistant", "Application started successfully", 
                    SimpleDateFormat("HH:mm:ss").format(Date())),
            LogEntry("DEBUG", "PhoneStateManager", "Phone state monitoring initialized", 
                    SimpleDateFormat("HH:mm:ss").format(Date(Date().time - 30000))),
            LogEntry("WARN", "SMSHandler", "High SMS volume detected", 
                    SimpleDateFormat("HH:mm:ss").format(Date(Date().time - 60000))),
            LogEntry("ERROR", "BackendClient", "Failed to connect to backend: timeout", 
                    SimpleDateFormat("HH:mm:ss").format(Date(Date().time - 120000))),
            LogEntry("INFO", "SyncService", "Preferences synchronized with backend", 
                    SimpleDateFormat("HH:mm:ss").format(Date(Date().time - 180000))),
            LogEntry("DEBUG", "AppDatabase", "Room database initialized", 
                    SimpleDateFormat("HH:mm:ss").format(Date(Date().time - 240000)))
        )
        
        logs.addAll(simulatedLogs)
        return logs.sortedByDescending { it.timestamp }
    }
    
    /**
     * Set log level filter
     */
    fun setLogLevelFilter(level: String) {
        val currentLogs = _uiState.value.logs
        val filteredLogs = if (level == "ALL") {
            currentLogs
        } else {
            currentLogs.filter { it.level == level }
        }
        
        _uiState.value = _uiState.value.copy(
            selectedLogLevel = level,
            logs = filteredLogs
        )
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs() {
        _uiState.value = _uiState.value.copy(
            logs = emptyList(),
            error = null
        )
    }
    
    /**
     * Export logs to string
     */
    fun exportLogs(): String {
        val logs = _uiState.value.logs
        val systemInfo = _uiState.value.systemInfo
        
        val exportData = buildString {
            appendLine("Voice Assistant Diagnostics Report")
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}")
            appendLine()
            
            appendLine("System Information:")
            appendLine("- Android Version: ${systemInfo.androidVersion} (API ${systemInfo.sdkVersion})")
            appendLine("- Device Model: ${systemInfo.deviceModel}")
            appendLine("- App Version: ${systemInfo.appVersion} (${systemInfo.appVersionCode})")
            appendLine("- Memory: ${systemInfo.totalMemory / (1024 * 1024)}MB total, ${systemInfo.availableMemory / (1024 * 1024)}MB available")
            appendLine("- Storage: ${systemInfo.totalStorage / (1024 * 1024 * 1024)}GB total, ${systemInfo.availableStorage / (1024 * 1024 * 1024)}GB available")
            appendLine("- Network: ${systemInfo.networkType} (${systemInfo.networkState})")
            appendLine("- Permissions: ${systemInfo.grantedPermissions}/${systemInfo.requestedPermissions} granted")
            appendLine()
            
            appendLine("Logs (${logs.size} entries):")
            appendLine("=".repeat(60))
            
            logs.forEach { log ->
                appendLine("[${log.level}] ${log.timestamp} ${log.tag}: ${log.message}")
            }
        }
        
        return exportData
    }
    
    /**
     * Copy logs to clipboard
     */
    fun copyLogsToClipboard() {
        try {
            val logs = exportLogs()
            val clipboard = android.content.ClipboardManager::class.java
                .getMethod("getSystemService", String::class.java)
                .invoke(null, android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            
            val clip = android.content.ClipData.newPlainText("Voice Assistant Logs", logs)
            clipboard.setPrimaryClip(clip)
            
            // Add success test result
            addTestResult("Copy to Clipboard", true, "Logs copied to clipboard successfully")
        } catch (e: Exception) {
            addTestResult("Copy to Clipboard", false, "Failed to copy: ${e.message}")
        }
    }
    
    /**
     * Test backend connection
     */
    fun testBackendConnection() {
        viewModelScope.launch {
            try {
                val userId = preferences.userId.first()
                if (userId == -1) {
                    addTestResult("Backend Connection", false, "User ID not configured")
                    return@launch
                }
                
                // Test basic connectivity by trying to get preferences
                val preferencesMap = backendClient.getPreferences(userId)
                
                addTestResult(
                    "Backend Connection", 
                    true, 
                    "Successfully connected to ${preferences.backendUrl}"
                )
            } catch (e: Exception) {
                addTestResult(
                    "Backend Connection", 
                    false, 
                    "Connection failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Test app permissions
     */
    fun testPermissions() {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.SEND_SMS
        )
        
        val results = requiredPermissions.map { permission ->
            val granted = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            permission to granted
        }
        
        val grantedCount = results.count { it.second }
        val totalCount = results.size
        
        val message = buildString {
            appendLine("$grantedCount/$totalCount permissions granted:")
            results.forEach { (permission, granted) ->
                appendLine("- ${permission.substringAfterLast('.')}: ${if (granted) "✓" else "✗"}")
            }
        }
        
        val success = grantedCount == totalCount
        addTestResult("Permission Check", success, message)
    }
    
    /**
     * Add a test result
     */
    private fun addTestResult(testName: String, success: Boolean, message: String) {
        val newResult = TestResult(
            testName = testName,
            success = success,
            message = message,
            timestamp = SimpleDateFormat("HH:mm:ss").format(Date())
        )
        
        val currentResults = _uiState.value.testResults
        _uiState.value = _uiState.value.copy(
            testResults = listOf(newResult) + currentResults.take(4) // Keep last 5 results
        )
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}