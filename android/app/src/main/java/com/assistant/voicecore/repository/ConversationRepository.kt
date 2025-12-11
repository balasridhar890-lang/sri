package com.assistant.voicecore.repository

import com.assistant.voicecore.model.ConversationRequest
import com.assistant.voicecore.model.ConversationResponse
import com.assistant.voicecore.model.SMSDecisionRequest
import com.assistant.voicecore.model.SMSDecisionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling API communication with the backend
 */
@Singleton
class ConversationRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun processConversation(request: ConversationRequest): Response<ConversationResponse> {
        return apiService.processConversation(request)
    }

    suspend fun makeSMSDecision(request: SMSDecisionRequest): Response<SMSDecisionResponse> {
        return apiService.makeSMSDecision(request)
    }

    suspend fun getConversationHistory(userId: Long, limit: Int = 50): Response<List<ConversationResponse>> {
        return apiService.getConversationHistory(userId, limit)
    }

    suspend fun getCallHistory(userId: Long, limit: Int = 50): Response<List<CallLogResponse>> {
        return apiService.getCallHistory(userId, limit)
    }

    suspend fun getSMSHistory(userId: Long, limit: Int = 50): Response<List<SMSLogResponse>> {
        return apiService.getSMSHistory(userId, limit)
    }
}

/**
 * API Service interface for backend communication
 */
interface ApiService {

    @POST("conversation/")
    suspend fun processConversation(@Body request: ConversationRequest): Response<ConversationResponse>

    @POST("sms/decision")
    suspend fun makeSMSDecision(@Body request: SMSDecisionRequest): Response<SMSDecisionResponse>

    @GET("history/{userId}/conversations")
    suspend fun getConversationHistory(
        @Path("userId") userId: Long,
        @retrofit2.http.Query("limit") limit: Int = 50
    ): Response<List<ConversationResponse>>

    @GET("history/{userId}/calls")
    suspend fun getCallHistory(
        @Path("userId") userId: Long,
        @retrofit2.http.Query("limit") limit: Int = 50
    ): Response<List<CallLogResponse>>

    @GET("history/{userId}/sms")
    suspend fun getSMSHistory(
        @Path("userId") userId: Long,
        @retrofit2.http.Query("limit") limit: Int = 50
    ): Response<List<SMSLogResponse>>
}

// Additional response models for history endpoints
data class CallLogResponse(
    val id: Long,
    val userId: Long,
    val incomingNumber: String,
    val duration: Long,
    val status: String,
    val answeredBy: String?,
    val transcript: String?,
    val createdAt: String,
    val answeredAt: String?
)

data class SMSLogResponse(
    val id: Long,
    val userId: Long,
    val incomingNumber: String,
    val incomingText: String,
    val decision: String,
    val replyText: String,
    val createdAt: String
)