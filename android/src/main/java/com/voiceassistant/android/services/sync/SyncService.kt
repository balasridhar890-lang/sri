package com.voiceassistant.android.services.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.voiceassistant.android.services.phone.CallLogger
import com.voiceassistant.android.services.sms.SMSHandler

/**
 * Service for periodically syncing local logs with backend
 */
@AndroidEntryPoint
class SyncService : Service(), LifecycleOwner {
    companion object {
        private const val TAG = "SyncService"
        private const val SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    @Inject
    lateinit var callLogger: CallLogger
    
    @Inject
    lateinit var smsHandler: SMSHandler
    
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private var isSyncing = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SyncService created")
        
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SyncService started")
        
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        
        // Start periodic sync
        startPeriodicSync()
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        Log.d(TAG, "SyncService destroyed")
        isSyncing = false
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    
    private fun startPeriodicSync() {
        lifecycleScope.launch {
            while (isSyncing) {
                delay(SYNC_INTERVAL_MS)
                performSync()
            }
        }
    }
    
    private suspend fun performSync() {
        Log.d(TAG, "Starting synchronization")
        
        try {
            // Sync call logs
            callLogger.syncAllPendingLogs()
            
            // Sync SMS logs
            smsHandler.syncAllPendingSMSLogs()
            
            Log.d(TAG, "Synchronization completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Synchronization failed", e)
        }
    }
    
    /**
     * Sync immediately (not waiting for next interval)
     */
    fun syncNow() {
        lifecycleScope.launch {
            performSync()
        }
    }
}
