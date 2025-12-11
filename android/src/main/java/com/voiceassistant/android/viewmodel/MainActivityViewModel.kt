package com.voiceassistant.android.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.permissions.PermissionManager
import com.voiceassistant.android.services.phone.PhoneStateService
import com.voiceassistant.android.services.sync.SyncService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI state for main activity
 */
data class MainActivityState(
    val isInitializing: Boolean = true,
    val permissionsGranted: Boolean = false,
    val servicesStarted: Boolean = false,
    val showPermissionDialog: Boolean = false,
    val missingPermissions: Array<String> = emptyArray(),
    val permissionRequestPending: Boolean = false,
    val error: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MainActivityState
        return isInitializing == other.isInitializing &&
                permissionsGranted == other.permissionsGranted &&
                servicesStarted == other.servicesStarted &&
                showPermissionDialog == other.showPermissionDialog &&
                missingPermissions.contentEquals(other.missingPermissions) &&
                permissionRequestPending == other.permissionRequestPending &&
                error == other.error
    }
    
    override fun hashCode(): Int {
        var result = isInitializing.hashCode()
        result = 31 * result + permissionsGranted.hashCode()
        result = 31 * result + servicesStarted.hashCode()
        result = 31 * result + showPermissionDialog.hashCode()
        result = 31 * result + missingPermissions.contentHashCode()
        result = 31 * result + permissionRequestPending.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}

/**
 * ViewModel for main activity
 */
@Singleton
class MainActivityViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val permissionManager: PermissionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "MainActivityViewModel"
    }
    
    private val _uiState = MutableStateFlow(MainActivityState())
    val uiState: StateFlow<MainActivityState> = _uiState.asStateFlow()
    
    init {
        Log.d(TAG, "MainActivityViewModel initialized")
    }
    
    /**
     * Check current permissions and update state
     */
    fun checkPermissions() {
        Log.d(TAG, "Checking permissions")
        
        viewModelScope.launch {
            try {
                val missingPermissions = permissionManager.getMissingPermissions(
                    PermissionManager.ALL_REQUIRED_PERMISSIONS
                )
                
                val permissionsGranted = missingPermissions.isEmpty()
                
                _uiState.value = _uiState.value.copy(
                    isInitializing = false,
                    permissionsGranted = permissionsGranted,
                    showPermissionDialog = !permissionsGranted,
                    missingPermissions = missingPermissions,
                    error = null
                )
                
                Log.d(TAG, "Permissions check completed. Missing: ${missingPermissions.toList()}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking permissions", e)
                _uiState.value = _uiState.value.copy(
                    isInitializing = false,
                    error = "Failed to check permissions: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Request permissions
     */
    fun requestPermissions(permissions: Array<String>) {
        Log.d(TAG, "Requesting permissions: ${permissions.toList()}")
        
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionRequestPending = true
        )
    }
    
    /**
     * Dismiss permission dialog
     */
    fun dismissPermissionDialog() {
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            permissionRequestPending = false
        )
    }
    
    /**
     * Start background services
     */
    fun startServices() {
        Log.d(TAG, "Starting services")
        
        viewModelScope.launch {
            try {
                // Start phone state monitoring service
                startPhoneStateService()
                
                // Start sync service
                startSyncService()
                
                _uiState.value = _uiState.value.copy(
                    servicesStarted = true
                )
                
                Log.d(TAG, "Services started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting services", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start services: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Start phone state monitoring service
     */
    private fun startPhoneStateService() {
        val phoneStateIntent = Intent(context, PhoneStateService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(phoneStateIntent)
        } else {
            context.startService(phoneStateIntent)
        }
        
        Log.d(TAG, "Phone state service started")
    }
    
    /**
     * Start sync service
     */
    private fun startSyncService() {
        val syncIntent = Intent(context, SyncService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(syncIntent)
        } else {
            context.startService(syncIntent)
        }
        
        Log.d(TAG, "Sync service started")
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}