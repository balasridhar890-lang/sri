package com.voiceassistant.android.network

import android.util.Log
import com.voiceassistant.android.config.AppConfig
import com.voiceassistant.android.repository.PreferencesSyncRequest
import com.voiceassistant.android.repository.PreferencesSyncResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * Retrofit interface for backend API communication
 */
interface BackendApi {
    @POST("/sms/decision")
    suspend fun makeSMSDecision(
        @Body request: SMSDecisionRequest
    ): SMSDecisionResponse
    
    @GET("/history/{user_id}")
    suspend fun getUserHistory(
        @Path("user_id") userId: Int
    ): HistoryResponse
    
    @GET("/history/{user_id}/calls")
    suspend fun getCallHistory(
        @Path("user_id") userId: Int
    ): List<CallLogResponse>
    
    @GET("/history/{user_id}/sms")
    suspend fun getSMSHistory(
        @Path("user_id") userId: Int
    ): List<SMSLogResponse>
    
    @POST("/preferences/sync")
    suspend fun syncPreferences(
        @Body request: PreferencesSyncRequest
    ): PreferencesSyncResponse
    
    @GET("/preferences/{user_id}")
    suspend fun getPreferences(
        @Path("user_id") userId: Int
    ): Map<String, Any>
}

/**
 * Request/Response models for backend communication
 */
@Serializable
data class SMSDecisionRequest(
    @SerialName("user_id")
    val user_id: Int,
    @SerialName("text")
    val text: String
)

@Serializable
data class SMSDecisionResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("user_id")
    val user_id: Int,
    @SerialName("incoming_text")
    val incoming_text: String,
    @SerialName("decision")
    val decision: String,
    @SerialName("reply_text")
    val reply_text: String,
    @SerialName("created_at")
    val created_at: String
)

@Serializable
data class HistoryResponse(
    @SerialName("conversation_logs")
    val conversation_logs: List<ConversationLogResponse>,
    @SerialName("call_logs")
    val call_logs: List<CallLogResponse>,
    @SerialName("sms_logs")
    val sms_logs: List<SMSLogResponse>
)

@Serializable
data class ConversationLogResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("user_id")
    val user_id: Int,
    @SerialName("input_text")
    val input_text: String,
    @SerialName("gpt_response")
    val gpt_response: String,
    @SerialName("input_tokens")
    val input_tokens: Int,
    @SerialName("output_tokens")
    val output_tokens: Int,
    @SerialName("processing_time_ms")
    val processing_time_ms: Float,
    @SerialName("model_used")
    val model_used: String,
    @SerialName("created_at")
    val created_at: String
)

@Serializable
data class CallLogResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("user_id")
    val user_id: Int,
    @SerialName("call_duration_seconds")
    val call_duration_seconds: Float,
    @SerialName("success")
    val success: Boolean,
    @SerialName("error_message")
    val error_message: String? = null,
    @SerialName("created_at")
    val created_at: String
)

@Serializable
data class SMSLogResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("user_id")
    val user_id: Int,
    @SerialName("incoming_text")
    val incoming_text: String,
    @SerialName("decision")
    val decision: String,
    @SerialName("reply_text")
    val reply_text: String,
    @SerialName("created_at")
    val created_at: String
)

/**
 * API request/response models for preferences
 */
@Serializable
data class PreferencesSyncRequest(
    val userId: Int,
    val preferences: Map<String, Any>
)

@Serializable
data class PreferencesSyncResponse(
    val success: Boolean,
    val message: String? = null,
    val preferences: Map<String, Any>? = null
)

/**
 * Backend client for API communication
 */
@Singleton
class BackendClient @Inject constructor(private val config: AppConfig) {
    companion object {
        private const val TAG = "BackendClient"
        private const val TIMEOUT_SECONDS = 30L
    }
    
    private val api: BackendApi by lazy {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        
        Retrofit.Builder()
            .baseUrl(config.backendUrl)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendApi::class.java)
    }
    
    suspend fun makeSMSDecision(request: SMSDecisionRequest): SMSDecisionResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Making SMS decision request for user ${request.user_id}")
                api.makeSMSDecision(request).also {
                    Log.d(TAG, "SMS decision received: ${it.decision}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error making SMS decision", e)
                throw e
            }
        }
    }
    
    suspend fun getUserHistory(userId: Int): HistoryResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching history for user $userId")
                api.getUserHistory(userId).also {
                    Log.d(TAG, "History retrieved: ${it.call_logs.size} calls, ${it.sms_logs.size} SMS")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user history", e)
                throw e
            }
        }
    }
    
    suspend fun getCallHistory(userId: Int): List<CallLogResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching call history for user $userId")
                api.getCallHistory(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching call history", e)
                throw e
            }
        }
    }
    
    suspend fun getSMSHistory(userId: Int): List<SMSLogResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching SMS history for user $userId")
                api.getSMSHistory(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching SMS history", e)
                throw e
            }
        }
    }
    
    suspend fun syncPreferences(request: PreferencesSyncRequest): PreferencesSyncResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Syncing preferences for user ${request.userId}")
                api.syncPreferences(request).also {
                    Log.d(TAG, "Preferences sync completed: ${it.success}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing preferences", e)
                throw e
            }
        }
    }
    
    suspend fun getPreferences(userId: Int): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching preferences for user $userId")
                api.getPreferences(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching preferences", e)
                throw e
            }
        }
    }
}
