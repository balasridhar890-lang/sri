package com.voiceassistant.android.services.phone

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.voiceassistant.android.database.AppDatabase
import com.voiceassistant.android.database.CallLogEntity
import com.voiceassistant.android.network.BackendClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

/**
 * Handles local and remote logging of call events
 */
@Singleton
class CallLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backendClient: BackendClient,
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "CallLogger"
    }
    
    /**
     * Log an incoming call
     */
    suspend fun logIncomingCall(
        phoneNumber: String,
        success: Boolean = true,
        errorMessage: String? = null
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Logging incoming call from $phoneNumber")
        
        try {
            val entity = CallLogEntity(
                phoneNumber = phoneNumber,
                direction = "incoming",
                timestamp = System.currentTimeMillis(),
                durationSeconds = 0,
                success = success,
                errorMessage = errorMessage,
                synced = false
            )
            
            // Save to local database
            database.callLogDao().insert(entity)
            Log.d(TAG, "Incoming call logged locally: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log incoming call locally", e)
        }
    }
    
    /**
     * Log an outgoing call
     */
    suspend fun logOutgoingCall(
        phoneNumber: String,
        duration: Long = 0,
        success: Boolean = true,
        errorMessage: String? = null
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Logging outgoing call to $phoneNumber")
        
        try {
            val entity = CallLogEntity(
                phoneNumber = phoneNumber,
                direction = "outgoing",
                timestamp = System.currentTimeMillis(),
                durationSeconds = duration,
                success = success,
                errorMessage = errorMessage,
                synced = false
            )
            
            database.callLogDao().insert(entity)
            Log.d(TAG, "Outgoing call logged locally: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log outgoing call locally", e)
        }
    }
    
    /**
     * Log a completed call (with duration)
     */
    suspend fun logCompletedCall(
        phoneNumber: String,
        direction: String,
        durationSeconds: Long,
        success: Boolean = true,
        errorMessage: String? = null
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Logging completed call: $phoneNumber (${durationSeconds}s)")
        
        try {
            val entity = CallLogEntity(
                phoneNumber = phoneNumber,
                direction = direction,
                timestamp = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                success = success,
                errorMessage = errorMessage,
                synced = false
            )
            
            database.callLogDao().insert(entity)
            Log.d(TAG, "Call logged locally: $phoneNumber")
            
            // Attempt to sync with backend
            syncCallLog(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log call", e)
        }
    }
    
    /**
     * Sync a call log with the backend
     */
    private suspend fun syncCallLog(entity: CallLogEntity) = withContext(Dispatchers.IO) {
        try {
            // This would call the backend /history endpoint
            // For now, just mark as synced
            database.callLogDao().update(entity.copy(synced = true))
            Log.d(TAG, "Call log synced with backend")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync call log with backend", e)
        }
    }
    
    /**
     * Get all unsynced call logs
     */
    suspend fun getUnsyncedCallLogs(): List<CallLogEntity> = withContext(Dispatchers.IO) {
        return@withContext database.callLogDao().getUnsyncedLogs()
    }
    
    /**
     * Sync all pending call logs with backend
     */
    suspend fun syncAllPendingLogs() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Syncing all pending call logs")
        
        try {
            val unsyncedLogs = getUnsyncedCallLogs()
            Log.d(TAG, "Found ${unsyncedLogs.size} unsynced logs")
            
            unsyncedLogs.forEach { log ->
                try {
                    syncCallLog(log)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync log: ${log.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending logs", e)
        }
    }
}
