package com.assistant.voicecore.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Data models for the VoiceCore application
 */

// User model
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val email: String,
    val phoneNumber: String? = null,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

// User preferences model
@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val language: String = "en",
    val ttsVoice: String = "default",
    val autoReplyEnabled: Boolean = true,
    val conversationTimeout: Int = 300, // 5 minutes
    val wakeWordSensitivity: Float = 0.7f,
    val backgroundServiceEnabled: Boolean = true,
    val autoCallAnswerEnabled: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

// Conversation log model
@Entity(tableName = "conversation_logs")
@Parcelize
data class ConversationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val inputText: String,
    val gptResponse: String,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val processingTimeMs: Long = 0,
    val modelUsed: String = "gpt-3.5-turbo",
    val createdAt: Date = Date()
) : Parcelable

// Call log model
@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val incomingNumber: String,
    val duration: Long = 0, // in seconds
    val status: CallStatus = CallStatus.MISSED,
    val answeredBy: String? = null, // "user", "assistant", "voicemail"
    val transcript: String? = null,
    val createdAt: Date = Date(),
    val answeredAt: Date? = null
)

enum class CallStatus {
    INCOMING,
    ANSWERED,
    MISSED,
    REJECTED,
    BUSY,
    VOICEMAIL
}

// SMS log model
@Entity(tableName = "sms_logs")
data class SMSLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val incomingNumber: String,
    val incomingText: String,
    val decision: String, // "yes", "no", "maybe"
    val replyText: String,
    val createdAt: Date = Date()
)

// Voice service status
data class VoiceServiceStatus(
    val isRunning: Boolean = false,
    val isListening: Boolean = false,
    val currentState: VoiceServiceState = VoiceServiceState.IDLE,
    val lastActivity: Date = Date(),
    val errorMessage: String? = null,
    val batteryOptimizationExempt: Boolean = false,
    val foregroundServiceActive: Boolean = false
)

enum class VoiceServiceState {
    IDLE,
    INITIALIZING,
    LISTENING,
    PROCESSING,
    SPEAKING,
    CALL_ANSWERING,
    ERROR
}

// Speech recognition result
data class SpeechResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val timestamp: Date = Date()
)

// Audio processing data
data class AudioData(
    val buffer: ByteArray,
    val sampleRate: Int,
    val channels: Int,
    val timestamp: Date = Date()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as AudioData
        
        return buffer.contentEquals(other.buffer) &&
                sampleRate == other.sampleRate &&
                channels == other.channels &&
                timestamp == other.timestamp
    }
    
    override fun hashCode(): Int {
        var result = buffer.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channels
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

// API request/response models
data class ConversationRequest(
    val userId: Long,
    val text: String
)

data class ConversationResponse(
    val id: Long,
    val userId: Long,
    val inputText: String,
    val gptResponse: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val processingTimeMs: Double,
    val modelUsed: String,
    val createdAt: String
)

data class SMSDecisionRequest(
    val userId: Long,
    val text: String
)

data class SMSDecisionResponse(
    val id: Long,
    val userId: Long,
    val incomingText: String,
    val decision: String,
    val replyText: String,
    val createdAt: String
)

// Error handling
data class VoiceCoreException(
    val message: String,
    val code: String,
    val cause: Throwable? = null
) : Exception(message, cause)

enum class VoiceErrorCode {
    MICROPHONE_PERMISSION_DENIED,
    SPEECH_RECOGNITION_ERROR,
    TTS_ERROR,
    NETWORK_ERROR,
    SERVICE_NOT_STARTED,
    INVALID_WAKE_WORD,
    TIMEOUT_ERROR,
    BATTERY_OPTIMIZATION_BLOCKED
}