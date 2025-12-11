package com.voiceassistant.android.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level integration with backend for all phone/SMS operations
 */
@Singleton
class BackendIntegration @Inject constructor(
    private val backendClient: BackendClient
) {
    companion object {
        private const val TAG = "BackendIntegration"
    }
    
    /**
     * Request SMS decision from backend
     * Returns default "no" on error to prevent accidental replies
     */
    suspend fun requestSMSDecision(
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
                
                Log.d(TAG, "Sending SMS decision request: user=$userId, text_length=${messageText.length}")
                
                val response = backendClient.makeSMSDecision(request)
                
                Log.d(TAG, "Backend decision: ${response.decision}, reply_length=${response.reply_text.length}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get SMS decision from backend, defaulting to 'no'", e)
                
                // Return safe default (don't reply) on error
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
     * Fetch user's call history from backend
     */
    suspend fun fetchCallHistory(userId: Int): List<CallLogResponse> {
        Log.d(TAG, "Fetching call history for user $userId")
        
        return withContext(Dispatchers.IO) {
            try {
                val history = backendClient.getCallHistory(userId)
                Log.d(TAG, "Fetched ${history.size} call logs")
                history
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch call history", e)
                emptyList()
            }
        }
    }
    
    /**
     * Fetch user's SMS history from backend
     */
    suspend fun fetchSMSHistory(userId: Int): List<SMSLogResponse> {
        Log.d(TAG, "Fetching SMS history for user $userId")
        
        return withContext(Dispatchers.IO) {
            try {
                val history = backendClient.getSMSHistory(userId)
                Log.d(TAG, "Fetched ${history.size} SMS logs")
                history
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch SMS history", e)
                emptyList()
            }
        }
    }
    
    /**
     * Fetch complete user history
     */
    suspend fun fetchCompleteHistory(userId: Int): HistoryResponse? {
        Log.d(TAG, "Fetching complete history for user $userId")
        
        return withContext(Dispatchers.IO) {
            try {
                val history = backendClient.getUserHistory(userId)
                Log.d(TAG, "Fetched complete history: " +
                    "${history.conversation_logs.size} conversations, " +
                    "${history.call_logs.size} calls, " +
                    "${history.sms_logs.size} SMS")
                history
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch complete history", e)
                null
            }
        }
    }
    
    /**
     * Check if backend is reachable
     */
    suspend fun checkBackendConnectivity(): Boolean {
        Log.d(TAG, "Checking backend connectivity")
        
        return withContext(Dispatchers.IO) {
            try {
                // Try to fetch a minimal request to check connectivity
                val userId = 1 // This would be from user preferences
                backendClient.getCallHistory(userId)
                Log.d(TAG, "Backend is reachable")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Backend is not reachable", e)
                false
            }
        }
    }
}
