package com.voiceassistant.android.repository

import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.network.BackendClient
import com.voiceassistant.android.network.PreferencesSyncRequest
import com.voiceassistant.android.network.PreferencesSyncResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class for user preferences that can be serialized for backend sync
 */
data class UserPreferences(
    val userId: Int,
    val backendUrl: String? = null,
    val autoAnswerEnabled: Boolean = true,
    val autoReplyEnabled: Boolean = true,
    val voiceActivationEnabled: Boolean = true,
    val syncEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val wakeWordSensitivity: Float = 0.5f,
    val voiceResponseVolume: Float = 0.8f,
    val voiceLanguage: String = "en-US",
    val voiceGender: String = "neutral",
    val callMonitoringEnabled: Boolean = true,
    val smsMonitoringEnabled: Boolean = true,
    val contactSyncEnabled: Boolean = true,
    val batteryOptimizationDisabled: Boolean = false,
    val debugModeEnabled: Boolean = false,
    val logLevel: String = "INFO",
    val performanceMonitoringEnabled: Boolean = false
)

/**
 * Repository for managing user preferences with backend sync
 */
@Singleton
class PreferencesRepository @Inject constructor(
    private val appPreferences: AppPreferences,
    private val backendClient: BackendClient
) {
    private val syncMutex = Mutex()
    private val localChanges = ConcurrentHashMap<String, Any>()
    private val lastSyncTimestamp = ConcurrentHashMap<String, Long>()
    
    /**
     * Get all preferences as a Flow
     */
    fun getAllPreferences(): Flow<UserPreferences> = flow {
        val userId = appPreferences.userId.first()
        if (userId == -1) {
            // Return defaults if no user configured
            emit(getDefaultPreferences())
            return@flow
        }
        
        // Try to sync with backend first
        try {
            syncWithBackend(userId)
        } catch (e: Exception) {
            // If sync fails, use local preferences
        }
        
        // Combine all preferences from DataStore
        val preferences = UserPreferences(
            userId = userId,
            backendUrl = appPreferences.backendUrl.first(),
            autoAnswerEnabled = appPreferences.autoAnswerEnabled.first(),
            autoReplyEnabled = appPreferences.autoReplyEnabled.first(),
            voiceActivationEnabled = appPreferences.voiceActivationEnabled.first(),
            syncEnabled = appPreferences.syncEnabled.first(),
            darkModeEnabled = appPreferences.darkModeEnabled.first(),
            accessibilityEnabled = appPreferences.accessibilityEnabled.first(),
            wakeWordSensitivity = appPreferences.wakeWordSensitivity.first(),
            voiceResponseVolume = appPreferences.voiceResponseVolume.first(),
            voiceLanguage = appPreferences.voiceLanguage.first(),
            voiceGender = appPreferences.voiceGender.first(),
            callMonitoringEnabled = appPreferences.callMonitoringEnabled.first(),
            smsMonitoringEnabled = appPreferences.smsMonitoringEnabled.first(),
            contactSyncEnabled = appPreferences.contactSyncEnabled.first(),
            batteryOptimizationDisabled = appPreferences.batteryOptimizationDisabled.first(),
            debugModeEnabled = appPreferences.debugModeEnabled.first(),
            logLevel = appPreferences.logLevel.first(),
            performanceMonitoringEnabled = appPreferences.performanceMonitoringEnabled.first()
        )
        
        emit(preferences)
    }
    
    /**
     * Update a single preference value
     */
    suspend fun updatePreference(key: String, value: Any) {
        val userId = appPreferences.userId.first()
        
        // Update local DataStore based on the key
        when (key) {
            "backendUrl" -> appPreferences.updateBackendUrl(value as String)
            "autoAnswerEnabled" -> appPreferences.updateAutoAnswerEnabled(value as Boolean)
            "autoReplyEnabled" -> appPreferences.updateAutoReplyEnabled(value as Boolean)
            "voiceActivationEnabled" -> appPreferences.updateVoiceActivationEnabled(value as Boolean)
            "syncEnabled" -> appPreferences.updateSyncEnabled(value as Boolean)
            "darkModeEnabled" -> appPreferences.updateDarkModeEnabled(value as Boolean)
            "accessibilityEnabled" -> appPreferences.updateAccessibilityEnabled(value as Boolean)
            "wakeWordSensitivity" -> appPreferences.updateWakeWordSensitivity(value as Float)
            "voiceResponseVolume" -> appPreferences.updateVoiceResponseVolume(value as Float)
            "voiceLanguage" -> appPreferences.updateVoiceLanguage(value as String)
            "voiceGender" -> appPreferences.updateVoiceGender(value as String)
            "callMonitoringEnabled" -> appPreferences.updateCallMonitoringEnabled(value as Boolean)
            "smsMonitoringEnabled" -> appPreferences.updateSmsMonitoringEnabled(value as Boolean)
            "contactSyncEnabled" -> appPreferences.updateContactSyncEnabled(value as Boolean)
            "batteryOptimizationDisabled" -> appPreferences.updateBatteryOptimizationDisabled(value as Boolean)
            "debugModeEnabled" -> appPreferences.updateDebugModeEnabled(value as Boolean)
            "logLevel" -> appPreferences.updateLogLevel(value as String)
            "performanceMonitoringEnabled" -> appPreferences.updatePerformanceMonitoringEnabled(value as Boolean)
        }
        
        // Mark for backend sync if user is configured and sync is enabled
        if (userId != -1 && appPreferences.syncEnabled.first()) {
            localChanges[key] = value
            lastSyncTimestamp[key] = System.currentTimeMillis()
            
            // Trigger background sync
            try {
                syncWithBackend(userId)
            } catch (e: Exception) {
                // Sync failed, changes will be queued for next sync attempt
            }
        }
    }
    
    /**
     * Update multiple preferences at once
     */
    suspend fun updatePreferences(preferences: Map<String, Any>) {
        preferences.forEach { (key, value) ->
            updatePreference(key, value)
        }
    }
    
    /**
     * Sync all unsynced changes with backend
     */
    suspend fun syncWithBackend(userId: Int): Boolean = syncMutex.withLock {
        if (localChanges.isEmpty()) {
            return@withLock true // Nothing to sync
        }
        
        try {
            // Prepare the request
            val request = PreferencesSyncRequest(
                userId = userId,
                preferences = localChanges.toMap()
            )
            
            // Sync with backend
            val response = backendClient.syncPreferences(request)
            
            if (response.success) {
                // Clear successfully synced changes
                localChanges.clear()
                lastSyncTimestamp.clear()
                return@withLock true
            } else {
                // Sync failed, keep changes for retry
                return@withLock false
            }
        } catch (e: Exception) {
            // Sync failed due to network error, keep changes for retry
            return@withLock false
        }
    }
    
    /**
     * Force sync all preferences (get latest from backend)
     */
    suspend fun forceSync(userId: Int): Boolean {
        return try {
            val backendPreferences = backendClient.getPreferences(userId)
            // Update local preferences with backend data
            backendPreferences.forEach { (key, value) ->
                try {
                    updatePreference(key, value)
                } catch (e: Exception) {
                    // Ignore individual preference update failures
                }
            }
            localChanges.clear()
            lastSyncTimestamp.clear()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get default preferences for new users
     */
    private fun getDefaultPreferences(): UserPreferences {
        return UserPreferences(
            userId = -1
        )
    }
    
    /**
     * Check if there are pending changes to sync
     */
    fun hasPendingChanges(): Boolean {
        return localChanges.isNotEmpty()
    }
    
    /**
     * Get count of pending changes
     */
    fun getPendingChangesCount(): Int {
        return localChanges.size
    }
    
    /**
     * Clear all pending changes (use with caution)
     */
    fun clearPendingChanges() {
        localChanges.clear()
        lastSyncTimestamp.clear()
    }
}