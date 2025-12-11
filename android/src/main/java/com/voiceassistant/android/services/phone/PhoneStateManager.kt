package com.voiceassistant.android.services.phone

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * Manages phone state changes and call interception
 */
@Singleton
class PhoneStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PhoneStateManager"
    }
    
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    
    sealed class PhoneState {
        data object Idle : PhoneState()
        data class IncomingCall(val phoneNumber: String) : PhoneState()
        data class OutgoingCall(val phoneNumber: String) : PhoneState()
        data class CallConnected(val phoneNumber: String, val startTime: Long) : PhoneState()
        data class CallDisconnected(val phoneNumber: String, val duration: Long) : PhoneState()
        data class CallFailed(val phoneNumber: String, val reason: String) : PhoneState()
    }
    
    private val _phoneState = MutableStateFlow<PhoneState>(PhoneState.Idle)
    val phoneState: StateFlow<PhoneState> = _phoneState
    
    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive
    
    private val _currentPhoneNumber = MutableStateFlow<String>("")
    val currentPhoneNumber: StateFlow<String> = _currentPhoneNumber
    
    private var callStartTime: Long = 0
    private var phoneStateListener: PhoneStateListener? = null
    
    /**
     * Start monitoring phone state changes
     */
    fun startMonitoring() {
        Log.d(TAG, "Starting phone state monitoring")
        
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                super.onCallStateChanged(state, incomingNumber)
                handleCallStateChange(state, incomingNumber)
            }
        }
        
        phoneStateListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }
    
    /**
     * Stop monitoring phone state changes
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping phone state monitoring")
        phoneStateListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        phoneStateListener = null
    }
    
    private fun handleCallStateChange(state: Int, incomingNumber: String?) {
        val number = incomingNumber ?: "Unknown"
        
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d(TAG, "Call ended for $number")
                if (_isCallActive.value) {
                    val duration = System.currentTimeMillis() - callStartTime
                    _phoneState.value = PhoneState.CallDisconnected(number, duration)
                    _isCallActive.value = false
                } else {
                    _phoneState.value = PhoneState.Idle
                }
                _currentPhoneNumber.value = ""
            }
            
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d(TAG, "Incoming call from $number")
                _phoneState.value = PhoneState.IncomingCall(number)
                _currentPhoneNumber.value = number
            }
            
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d(TAG, "Call active with $number")
                callStartTime = System.currentTimeMillis()
                _isCallActive.value = true
                
                // Determine if incoming or outgoing based on previous state
                val previousState = _phoneState.value
                _phoneState.value = if (previousState is PhoneState.IncomingCall) {
                    PhoneState.CallConnected(number, callStartTime)
                } else {
                    PhoneState.OutgoingCall(number)
                }
            }
        }
    }
    
    /**
     * Attempt to auto-answer an incoming call
     * Note: This requires ANSWER_PHONE_CALLS permission and Android 8+
     */
    fun autoAnswerCall(phoneNumber: String): Boolean {
        return try {
            Log.d(TAG, "Attempting to auto-answer call from $phoneNumber")
            // The actual auto-answer is handled in CallReceiver with TelecomManager
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-answer call", e)
            false
        }
    }
    
    /**
     * End current active call
     */
    fun endCall(): Boolean {
        return try {
            Log.d(TAG, "Ending active call")
            // This would be implemented using TelecomManager in CallReceiver
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end call", e)
            false
        }
    }
    
    /**
     * Get current call duration in seconds
     */
    fun getCurrentCallDuration(): Long {
        return if (_isCallActive.value) {
            (System.currentTimeMillis() - callStartTime) / 1000
        } else {
            0L
        }
    }
    
    /**
     * Check if there's an active call
     */
    fun isCallActive(): Boolean = _isCallActive.value
    
    /**
     * Get current phone state
     */
    fun getCurrentState(): PhoneState = _phoneState.value
}
