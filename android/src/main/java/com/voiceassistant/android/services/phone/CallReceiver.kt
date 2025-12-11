package com.voiceassistant.android.services.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles incoming call events and initiates auto-answer flow
 */
@AndroidEntryPoint
class CallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallReceiver"
    }
    
    @Inject
    lateinit var phoneStateManager: PhoneStateManager
    
    @Inject
    lateinit var callLogger: CallLogger
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received call intent: ${intent.action}")
        
        val scope = CoroutineScope(Dispatchers.Default)
        
        when (intent.action) {
            "android.intent.action.PHONE_STATE" -> {
                handlePhoneStateChange(context, intent, scope)
            }
            "android.intent.action.NEW_OUTGOING_CALL" -> {
                handleOutgoingCall(context, intent, scope)
            }
        }
    }
    
    private fun handlePhoneStateChange(context: Context, intent: Intent, scope: CoroutineScope) {
        val state = intent.getStringExtra("state")
        val incomingNumber = intent.getStringExtra("incoming_number")
        
        Log.d(TAG, "Phone state changed: $state for number: $incomingNumber")
        
        when (state) {
            "RINGING" -> {
                // Incoming call detected
                incomingNumber?.let {
                    scope.launch {
                        handleIncomingCall(context, it)
                    }
                }
            }
            "OFFHOOK" -> {
                // Call is active - auto-answer if needed
                Log.d(TAG, "Call is now active")
            }
            "IDLE" -> {
                // Call ended
                Log.d(TAG, "Call ended")
            }
        }
    }
    
    private fun handleOutgoingCall(context: Context, intent: Intent, scope: CoroutineScope) {
        val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        Log.d(TAG, "Outgoing call to: $outgoingNumber")
        
        outgoingNumber?.let {
            scope.launch {
                callLogger.logOutgoingCall(it, success = true)
            }
        }
    }
    
    private suspend fun handleIncomingCall(context: Context, phoneNumber: String) {
        Log.d(TAG, "Handling incoming call from: $phoneNumber")
        
        try {
            // Check if auto-answer is enabled
            val shouldAutoAnswer = true // This would be fetched from preferences
            
            if (shouldAutoAnswer) {
                // Start phone state service
                startPhoneStateService(context)
                
                // Log the incoming call
                callLogger.logIncomingCall(phoneNumber)
                
                // Attempt auto-answer
                autoAnswerCall(context, phoneNumber)
                
                // Start voice assistant service
                startVoiceAssistantService(context, phoneNumber)
            } else {
                Log.d(TAG, "Auto-answer is disabled, logging call only")
                callLogger.logIncomingCall(phoneNumber)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming call", e)
            callLogger.logIncomingCall(phoneNumber, success = false, errorMessage = e.message)
        }
    }
    
    private fun autoAnswerCall(context: Context, phoneNumber: String) {
        Log.d(TAG, "Attempting to auto-answer call from $phoneNumber")
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                
                telecomManager?.let {
                    // Answer the call
                    it.acceptRingingCall()
                    Log.d(TAG, "Call answered via TelecomManager")
                }
            } else {
                Log.w(TAG, "Auto-answer requires Android 8+")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-answer call", e)
        }
    }
    
    private fun startPhoneStateService(context: Context) {
        Log.d(TAG, "Starting PhoneStateService")
        try {
            val intent = Intent(context, PhoneStateService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PhoneStateService", e)
        }
    }
    
    private fun startVoiceAssistantService(context: Context, phoneNumber: String) {
        Log.d(TAG, "Starting voice assistant for call from $phoneNumber")
        // This would trigger the voice assistant to handle the call
        // Implementation depends on the voice assistant framework
    }
}
