package com.voiceassistant.android.services.phone

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for continuous phone state monitoring
 */
@AndroidEntryPoint
class PhoneStateService : Service(), LifecycleOwner {
    companion object {
        private const val TAG = "PhoneStateService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "phone_state_channel"
    }
    
    @Inject
    lateinit var phoneStateManager: PhoneStateManager
    
    @Inject
    lateinit var callLogger: CallLogger
    
    private lateinit var lifecycleRegistry: LifecycleRegistry
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PhoneStateService created")
        
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        // Start monitoring phone state
        phoneStateManager.startMonitoring()
        
        // Observe phone state changes
        observePhoneState()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "PhoneStateService started")
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        Log.d(TAG, "PhoneStateService destroyed")
        phoneStateManager.stopMonitoring()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    
    private fun observePhoneState() {
        lifecycleScope.launch {
            phoneStateManager.phoneState.collect { state ->
                Log.d(TAG, "Phone state changed: $state")
                
                when (state) {
                    is PhoneStateManager.PhoneState.IncomingCall -> {
                        handleIncomingCall(state.phoneNumber)
                    }
                    is PhoneStateManager.PhoneState.CallConnected -> {
                        handleCallConnected(state.phoneNumber)
                    }
                    is PhoneStateManager.PhoneState.CallDisconnected -> {
                        handleCallDisconnected(state.phoneNumber, state.duration)
                    }
                    is PhoneStateManager.PhoneState.CallFailed -> {
                        handleCallFailed(state.phoneNumber, state.reason)
                    }
                    else -> {
                        Log.d(TAG, "Ignoring state: $state")
                    }
                }
            }
        }
    }
    
    private suspend fun handleIncomingCall(phoneNumber: String) {
        Log.d(TAG, "Handling incoming call from $phoneNumber")
        callLogger.logIncomingCall(phoneNumber)
    }
    
    private suspend fun handleCallConnected(phoneNumber: String) {
        Log.d(TAG, "Call connected with $phoneNumber")
        // Update notification with ongoing call
        updateNotification("Call active with $phoneNumber")
    }
    
    private suspend fun handleCallDisconnected(phoneNumber: String, duration: Long) {
        Log.d(TAG, "Call disconnected with $phoneNumber (duration: ${duration}ms)")
        val durationSeconds = duration / 1000
        callLogger.logCompletedCall(phoneNumber, "incoming", durationSeconds)
    }
    
    private suspend fun handleCallFailed(phoneNumber: String, reason: String) {
        Log.e(TAG, "Call failed with $phoneNumber: $reason")
        callLogger.logIncomingCall(phoneNumber, success = false, errorMessage = reason)
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Voice Assistant")
        .setContentText("Monitoring calls...")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
    
    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Assistant")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    
    private val notificationManager by lazy {
        getSystemService(android.app.NotificationManager::class.java)
    }
}
