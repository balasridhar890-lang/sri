package com.voiceassistant.android.services.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles incoming SMS messages and initiates auto-reply workflow
 */
@AndroidEntryPoint
class SMSReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SMSReceiver"
    }
    
    @Inject
    lateinit var smsHandler: SMSHandler
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS received")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use SmsMessage.createFromPdu with a SmsMessage.MessageCreator callback
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            messages?.forEach { message ->
                val sender = message.displayOriginatingAddress
                val body = message.displayMessageBody
                
                Log.d(TAG, "SMS from: $sender, Body: ${body.take(50)}...")
                
                val scope = CoroutineScope(Dispatchers.Default)
                scope.launch {
                    smsHandler.handleIncomingSMS(sender, body, context)
                }
            }
        }
    }
}
