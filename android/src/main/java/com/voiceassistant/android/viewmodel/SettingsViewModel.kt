package com.voiceassistant.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.repository.PreferencesRepository
import com.voiceassistant.android.repository.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI state for settings screen
 */
data class SettingsState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences? = null,
    val hasUnsavedChanges: Boolean = false,
    val pendingChangesCount: Int = 0,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val syncError: String? = null,
    val saveError: String? = null
)

/**
 * ViewModel for settings screen
 */
@Singleton
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Load current settings
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = SettingsState(isLoading = true)
                
                val userPreferences = preferencesRepository.getAllPreferences().first()
                
                _uiState.value = SettingsState(
                    isLoading = false,
                    preferences = userPreferences,
                    lastSyncTime = preferences.lastBackupTimestamp.first()
                )
            } catch (e: Exception) {
                _uiState.value = SettingsState(
                    isLoading = false,
                    saveError = "Failed to load settings: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update a setting value
     */
    suspend fun updateSetting(key: String, value: Any) {
        try {
            preferencesRepository.updatePreference(key, value)
            val currentPreferences = preferencesRepository.getAllPreferences().first()
            
            _uiState.value = _uiState.value.copy(
                preferences = currentPreferences,
                hasUnsavedChanges = preferencesRepository.hasPendingChanges(),
                pendingChangesCount = preferencesRepository.getPendingChangesCount(),
                syncError = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                saveError = "Failed to update setting: ${e.message}"
            )
        }
    }
    
    /**
     * Update multiple settings at once
     */
    suspend fun updateSettings(settings: Map<String, Any>) {
        try {
            preferencesRepository.updatePreferences(settings)
            val currentPreferences = preferencesRepository.getAllPreferences().first()
            
            _uiState.value = _uiState.value.copy(
                preferences = currentPreferences,
                hasUnsavedChanges = preferencesRepository.hasPendingChanges(),
                syncError = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                saveError = "Failed to update settings: ${e.message}"
            )
        }
    }
    
    /**
     * Update API configuration
     */
    suspend fun updateApiConfig(url: String, userId: Int) {
        val settings = mapOf(
            "backendUrl" to url,
            "userId" to userId
        )
        updateSettings(settings)
    }
    
    /**
     * Update API key/token
     */
    suspend fun updateApiCredentials(apiKey: String?, apiToken: String?) {
        try {
            if (apiKey != null) {
                preferences.updateApiKey(apiKey)
            }
            if (apiToken != null) {
                preferences.updateApiToken(apiToken)
            }
            
            val currentPreferences = preferencesRepository.getAllPreferences().first()
            _uiState.value = _uiState.value.copy(
                preferences = currentPreferences,
                hasUnsavedChanges = preferencesRepository.hasPendingChanges(),
                syncError = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                saveError = "Failed to update API credentials: ${e.message}"
            )
        }
    }
    
    /**
     * Update voice settings
     */
    suspend fun updateVoiceSettings(
        sensitivity: Float,
        volume: Float,
        language: String,
        gender: String
    ) {
        val settings = mapOf(
            "wakeWordSensitivity" to sensitivity,
            "voiceResponseVolume" to volume,
            "voiceLanguage" to language,
            "voiceGender" to gender
        )
        updateSettings(settings)
    }
    
    /**
     * Update feature toggles
     */
    suspend fun updateFeatureToggles(
        autoAnswer: Boolean,
        autoReply: Boolean,
        voiceActivation: Boolean,
        callMonitoring: Boolean,
        smsMonitoring: Boolean,
        contactSync: Boolean
    ) {
        val settings = mapOf(
            "autoAnswerEnabled" to autoAnswer,
            "autoReplyEnabled" to autoReply,
            "voiceActivationEnabled" to voiceActivation,
            "callMonitoringEnabled" to callMonitoring,
            "smsMonitoringEnabled" to smsMonitoring,
            "contactSyncEnabled" to contactSync
        )
        updateSettings(settings)
    }
    
    /**
     * Update UI preferences
     */
    suspend fun updateUiPreferences(
        darkMode: Boolean,
        accessibility: Boolean
    ) {
        val settings = mapOf(
            "darkModeEnabled" to darkMode,
            "accessibilityEnabled" to accessibility
        )
        updateSettings(settings)
    }
    
    /**
     * Update debug settings
     */
    suspend fun updateDebugSettings(
        debugMode: Boolean,
        logLevel: String,
        performanceMonitoring: Boolean
    ) {
        val settings = mapOf(
            "debugModeEnabled" to debugMode,
            "logLevel" to logLevel,
            "performanceMonitoringEnabled" to performanceMonitoring
        )
        updateSettings(settings)
    }
    
    /**
     * Sync settings with backend
     */
    fun syncWithBackend() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true, syncError = null)
                
                val userId = preferences.userId.first()
                if (userId == -1) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncError = "User ID not configured. Please set up your account first."
                    )
                    return@launch
                }
                
                val success = preferencesRepository.syncWithBackend(userId)
                
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        hasUnsavedChanges = preferencesRepository.hasPendingChanges(),
                        lastSyncTime = System.currentTimeMillis()
                    )
                    // Update local backup timestamp
                    preferences.updateLastBackupTimestamp(System.currentTimeMillis())
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncError = "Sync failed. Changes will be synced later."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncError = "Sync error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Force sync (pull from backend)
     */
    fun forceSyncFromBackend() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSyncing = true, syncError = null)
                
                val userId = preferences.userId.first()
                if (userId == -1) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncError = "User ID not configured."
                    )
                    return@launch
                }
                
                val success = preferencesRepository.forceSync(userId)
                
                if (success) {
                    // Reload preferences after successful sync
                    loadSettings()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncError = "Failed to sync from backend."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncError = "Sync error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Reset all preferences
                preferences.resetAll()
                preferencesRepository.clearPendingChanges()
                
                // Reload settings
                loadSettings()
            } catch (e: Exception) {
                _uiState.value = SettingsState(
                    isLoading = false,
                    saveError = "Failed to reset settings: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Export settings to string (for backup)
     */
    suspend fun exportSettings(): String {
        val currentPreferences = preferencesRepository.getAllPreferences().first()
        return """
            {
                "backendUrl": "${currentPreferences.backendUrl ?: ""}",
                "autoAnswerEnabled": ${currentPreferences.autoAnswerEnabled},
                "autoReplyEnabled": ${currentPreferences.autoReplyEnabled},
                "voiceActivationEnabled": ${currentPreferences.voiceActivationEnabled},
                "syncEnabled": ${currentPreferences.syncEnabled},
                "darkModeEnabled": ${currentPreferences.darkModeEnabled},
                "accessibilityEnabled": ${currentPreferences.accessibilityEnabled},
                "wakeWordSensitivity": ${currentPreferences.wakeWordSensitivity},
                "voiceResponseVolume": ${currentPreferences.voiceResponseVolume},
                "voiceLanguage": "${currentPreferences.voiceLanguage}",
                "voiceGender": "${currentPreferences.voiceGender}",
                "callMonitoringEnabled": ${currentPreferences.callMonitoringEnabled},
                "smsMonitoringEnabled": ${currentPreferences.smsMonitoringEnabled},
                "contactSyncEnabled": ${currentPreferences.contactSyncEnabled},
                "batteryOptimizationDisabled": ${currentPreferences.batteryOptimizationDisabled},
                "debugModeEnabled": ${currentPreferences.debugModeEnabled},
                "logLevel": "${currentPreferences.logLevel}",
                "performanceMonitoringEnabled": ${currentPreferences.performanceMonitoringEnabled},
                "exportedAt": ${System.currentTimeMillis()}
            }
        """.trimIndent()
    }
    
    /**
     * Import settings from string
     */
    suspend fun importSettings(settingsJson: String) {
        try {
            // Parse JSON and update preferences
            // This is a simplified implementation - in production, you'd want proper JSON parsing
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // For now, just reload settings
            loadSettings()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                saveError = "Failed to import settings: ${e.message}"
            )
        }
    }
    
    /**
     * Check if there are pending changes to sync
     */
    fun hasPendingChanges(): Boolean {
        return preferencesRepository.hasPendingChanges()
    }
    
    /**
     * Get pending changes count
     */
    fun getPendingChangesCount(): Int {
        return preferencesRepository.getPendingChangesCount()
    }
    
    /**
     * Clear save/sync error messages
     */
    fun clearErrors() {
        _uiState.value = _uiState.value.copy(
            syncError = null,
            saveError = null
        )
    }
}