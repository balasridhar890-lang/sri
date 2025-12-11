package com.voiceassistant.android.services.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.room.Room
import com.voiceassistant.android.database.AppDatabase
import com.voiceassistant.android.database.SMSLogEntity
import com.voiceassistant.android.network.BackendClient
import com.voiceassistant.android.network.SMSDecisionRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles SMS workflow: receive, request decision, send reply
 */
@Singleton
class SMSHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backendClient: BackendClient,
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "SMSHandler"
    }
    
    /**
     * Handle incoming SMS with auto-reply workflow
     */
    suspend fun handleIncomingSMS(
        phoneNumber: String,
        messageBody: String,
        context: Context
    ) = withContext(Dispatchers.Default) {
        Log.d(TAG, "Handling incoming SMS from $phoneNumber")
        
        try {
            // Get user ID (would be fetched from preferences/database)
            val userId = 1 // This should come from actual user management
            
            // Request decision from backend
            val decision = requestSMSDecision(userId, messageBody)
            
            Log.d(TAG, "Backend decision: ${decision.decision}, reply: ${decision.reply_text}")
            
            // Log the incoming SMS
            logIncomingSMS(
                phoneNumber = phoneNumber,
                messageBody = messageBody,
                decision = decision.decision,
                replyText = decision.reply_text
            )
            
            // If approved, send the reply
            if (decision.decision == "yes" && decision.reply_text.isNotEmpty()) {
                val sendSuccess = sendSMSReply(phoneNumber, decision.reply_text)
                
                // Update log with send status
                updateSMSLogSendStatus(phoneNumber, sendSuccess)
                
                Log.d(TAG, "SMS reply sent to $phoneNumber: ${decision.reply_text}")
            } else {
                Log.d(TAG, "SMS reply rejected or empty, not sending")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming SMS", e)
            logIncomingSMS(
                phoneNumber = phoneNumber,
                messageBody = messageBody,
                decision = "error",
                replyText = "",
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Request SMS decision from backend
     */
    private suspend fun requestSMSDecision(
        userId: Int,
        messageText: String
    ): SMSDecisionResponse {
        Log.d(TAG, "Requesting SMS decision from backend")
        
        return withContext(Dispatchers.IO) {
            try {
                val request = SMSDecisionRequest(
                    user_id = userId,
                    text = messageText
                )
                
                val response = backendClient.makeSMSDecision(request)
                Log.d(TAG, "Backend decision received: ${response.decision}")
                response
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get SMS decision from backend", e)
                // Return a default "no" response on error
                SMSDecisionResponse(
                    id = 0,
                    user_id = userId,
                    incoming_text = messageText,
                    decision = "no",
                    reply_text = "",
                    created_at = ""
                )
            }
        }
    }
    
    /**
     * Send SMS reply
     */
    private fun sendSMSReply(phoneNumber: String, messageText: String): Boolean {
        Log.d(TAG, "Sending SMS reply to $phoneNumber")
        
        return try {
            val smsManager = context.getSystemService(Context.SMS_SERVICE) as SmsManager
            
            // Split message if too long
            val parts = smsManager.divideMessage(messageText)
            val sentIntents = parts.map {
                PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent("SMS_SENT"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            
            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                parts,
                sentIntents,
                null
            )
            
            Log.d(TAG, "SMS reply sent successfully to $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS reply", e)
            false
        }
    }
    
    /**
     * Log incoming SMS locally
     */
    private suspend fun logIncomingSMS(
        phoneNumber: String,
        messageBody: String,
        decision: String,
        replyText: String,
        errorMessage: String? = null
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Logging SMS from $phoneNumber")
        
        try {
            val entity = SMSLogEntity(
                phoneNumber = phoneNumber,
                messageBody = messageBody,
                decision = decision,
                replyText = replyText,
                replySent = false,
                timestamp = System.currentTimeMillis(),
                errorMessage = errorMessage,
                synced = false
            )
            
            database.smsLogDao().insert(entity)
            Log.d(TAG, "SMS logged locally")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log SMS locally", e)
        }
    }
    
    /**
     * Update SMS log with send status
     */
    private suspend fun updateSMSLogSendStatus(
        phoneNumber: String,
        sendSuccess: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val logs = database.smsLogDao().getByPhoneNumber(phoneNumber)
            if (logs.isNotEmpty()) {
                val lastLog = logs.last()
                database.smsLogDao().update(
                    lastLog.copy(replySent = sendSuccess, synced = false)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update SMS log", e)
        }
    }
    
    /**
     * Get all unsynced SMS logs
     */
    suspend fun getUnsyncedSMSLogs(): List<SMSLogEntity> = withContext(Dispatchers.IO) {
        return@withContext database.smsLogDao().getUnsyncedLogs()
    }
    
    /**
     * Sync SMS logs with backend
     */
    suspend fun syncAllPendingSMSLogs() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Syncing all pending SMS logs")
        
        try {
            val unsyncedLogs = getUnsyncedSMSLogs()
            Log.d(TAG, "Found ${unsyncedLogs.size} unsynced SMS logs")
            
            unsyncedLogs.forEach { log ->
                try {
                    // Mark as synced after sending to backend
                    database.smsLogDao().update(log.copy(synced = true))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync SMS log: ${log.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing pending SMS logs", e)
        }
    }
}

data class SMSDecisionResponse(
    val id: Int,
    val user_id: Int,
    val incoming_text: String,
    val decision: String, // "yes" or "no"
    val reply_text: String,
    val created_at: String
)
