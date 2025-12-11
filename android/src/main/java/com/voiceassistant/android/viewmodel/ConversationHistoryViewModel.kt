package com.voiceassistant.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.android.database.CallLogDao
import com.voiceassistant.android.database.SMSLogDao
import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.network.BackendClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI state for conversation history screen
 */
data class ConversationHistoryState(
    val isLoading: Boolean = true,
    val conversations: List<ConversationItem> = emptyList(),
    val callLogs: List<CallHistoryItem> = emptyList(),
    val smsLogs: List<SMSHistoryItem> = emptyList(),
    val selectedTab: HistoryTab = HistoryTab.ALL,
    val searchQuery: String = "",
    val dateFilter: DateFilter = DateFilter.ALL,
    val error: String? = null
)

/**
 * History tabs
 */
enum class HistoryTab(val displayName: String) {
    ALL("All"),
    CALLS("Calls"),
    SMS("SMS"),
    CONVERSATIONS("Conversations")
}

/**
 * Date filters
 */
enum class DateFilter(val displayName: String, val days: Int) {
    ALL("All Time", -1),
    TODAY("Today", 0),
    WEEK("Last Week", 7),
    MONTH("Last Month", 30),
    THREE_MONTHS("Last 3 Months", 90)
}

/**
 * Conversation item (SMS exchanges)
 */
data class ConversationItem(
    val id: String,
    val contactName: String,
    val contactNumber: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,
    val messages: List<Message> = emptyList()
) {
    val formattedTime: String = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        .format(Date(lastMessageTime))
}

/**
 * Individual message in a conversation
 */
data class Message(
    val id: String,
    val content: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val messageType: MessageType,
    val status: MessageStatus
)

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    SYSTEM
}

enum class MessageStatus {
    SENT,
    DELIVERED,
    READ,
    FAILED
}

/**
 * Call history item
 */
data class CallHistoryItem(
    val id: String,
    val phoneNumber: String,
    val contactName: String? = null,
    val direction: CallDirection,
    val timestamp: Long,
    val durationSeconds: Long,
    val success: Boolean,
    val wasAutoAnswered: Boolean = false
) {
    val formattedTime: String = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        .format(Date(timestamp))
    val formattedDuration: String = if (durationSeconds > 0) {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        "${minutes}m ${seconds}s"
    } else "0s"
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

/**
 * SMS history item
 */
data class SMSHistoryItem(
    val id: String,
    val phoneNumber: String,
    val contactName: String? = null,
    val messageBody: String,
    val timestamp: Long,
    val wasAutoReplied: Boolean,
    val replyText: String? = null,
    val decision: String
) {
    val formattedTime: String = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        .format(Date(timestamp))
}

/**
 * ViewModel for conversation history
 */
@Singleton
class ConversationHistoryViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val backendClient: BackendClient,
    private val callLogDao: CallLogDao,
    private val smsLogDao: SMSLogDao,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ConversationHistoryState())
    val uiState: StateFlow<ConversationHistoryState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    /**
     * Load conversation history from local database and backend
     */
    private fun loadHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val userId = preferences.userId.first()
                
                // Load from local database
                val localCallLogs = getLocalCallHistory()
                val localSMSLogs = getLocalSMSHistory()
                
                // Load from backend if user is configured
                if (userId != -1) {
                    try {
                        val backendHistory = backendClient.getUserHistory(userId)
                        // Merge with local data
                        loadHistoryFromBackend(backendHistory)
                    } catch (e: Exception) {
                        // Backend failed, continue with local data
                    }
                }
                
                // Build conversation items from SMS logs
                val conversations = buildConversations(localSMSLogs)
                
                _uiState.value = ConversationHistoryState(
                    isLoading = false,
                    conversations = conversations,
                    callLogs = localCallLogs,
                    smsLogs = localSMSLogs
                )
            } catch (e: Exception) {
                _uiState.value = ConversationHistoryState(
                    isLoading = false,
                    error = e.message ?: "Failed to load history"
                )
            }
        }
    }
    
    /**
     * Get call history from local database
     */
    private suspend fun getLocalCallHistory(): List<CallHistoryItem> {
        return try {
            val logs = callLogDao.getRecentLogs(100)
            logs.map { entity ->
                CallHistoryItem(
                    id = entity.id.toString(),
                    phoneNumber = entity.phoneNumber,
                    direction = if (entity.direction == "incoming") CallDirection.INCOMING else CallDirection.OUTGOING,
                    timestamp = entity.timestamp,
                    durationSeconds = entity.durationSeconds,
                    success = entity.success
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get SMS history from local database
     */
    private suspend fun getLocalSMSHistory(): List<SMSHistoryItem> {
        return try {
            val logs = smsLogDao.getRecentLogs(100)
            logs.map { entity ->
                SMSHistoryItem(
                    id = entity.id.toString(),
                    phoneNumber = entity.phoneNumber,
                    messageBody = entity.messageBody,
                    timestamp = entity.timestamp,
                    wasAutoReplied = entity.decision == "yes",
                    replyText = if (entity.decision == "yes") entity.replyText else null,
                    decision = entity.decision
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Build conversation threads from SMS logs
     */
    private fun buildConversations(smsLogs: List<SMSHistoryItem>): List<ConversationItem> {
        val conversationMap = mutableMapOf<String, ConversationItem>()
        
        smsLogs.forEach { sms ->
            val key = sms.phoneNumber
            
            val existing = conversationMap[key]
            if (existing == null) {
                // Create new conversation
                conversationMap[key] = ConversationItem(
                    id = key,
                    contactName = sms.contactName ?: sms.phoneNumber,
                    contactNumber = sms.phoneNumber,
                    lastMessage = sms.messageBody,
                    lastMessageTime = sms.timestamp,
                    messages = listOf(
                        Message(
                            id = sms.id,
                            content = sms.messageBody,
                            timestamp = sms.timestamp,
                            isFromMe = false,
                            messageType = MessageType.TEXT,
                            status = MessageStatus.READ
                        )
                    )
                )
            } else {
                // Update existing conversation
                val updatedMessages = existing.messages + Message(
                    id = sms.id,
                    content = sms.messageBody,
                    timestamp = sms.timestamp,
                    isFromMe = false,
                    messageType = MessageType.TEXT,
                    status = MessageStatus.READ
                )
                
                conversationMap[key] = existing.copy(
                    lastMessage = sms.messageBody,
                    lastMessageTime = maxOf(existing.lastMessageTime, sms.timestamp),
                    messages = updatedMessages
                )
            }
        }
        
        return conversationMap.values.sortedByDescending { it.lastMessageTime }
    }
    
    /**
     * Load history from backend
     */
    private suspend fun loadHistoryFromBackend(history: com.voiceassistant.android.network.HistoryResponse) {
        // This would sync backend data with local database
        // For now, just update the UI state with backend data if needed
    }
    
    /**
     * Change selected tab
     */
    fun selectTab(tab: HistoryTab) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(selectedTab = tab)
        applyFilters()
    }
    
    /**
     * Search conversations and logs
     */
    fun search(query: String) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(searchQuery = query)
        applyFilters()
    }
    
    /**
     * Change date filter
     */
    fun setDateFilter(filter: DateFilter) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(dateFilter = filter)
        applyFilters()
    }
    
    /**
     * Apply current filters to data
     */
    private fun applyFilters() {
        val currentState = _uiState.value
        
        val filteredConversations = filterByDate(currentState.conversations, currentState.dateFilter)
        val filteredCallLogs = filterCallLogsByDate(currentState.callLogs, currentState.dateFilter)
        val filteredSMSLogs = filterSMSLogsByDate(currentState.smsLogs, currentState.dateFilter)
        
        val searchFilteredConversations = if (currentState.searchQuery.isEmpty()) {
            filteredConversations
        } else {
            filteredConversations.filter { conversation ->
                conversation.contactName.contains(currentState.searchQuery, ignoreCase = true) ||
                conversation.contactNumber.contains(currentState.searchQuery, ignoreCase = true) ||
                conversation.lastMessage.contains(currentState.searchQuery, ignoreCase = true)
            }
        }
        
        val searchFilteredCallLogs = if (currentState.searchQuery.isEmpty()) {
            filteredCallLogs
        } else {
            filteredCallLogs.filter { call ->
                call.phoneNumber.contains(currentState.searchQuery, ignoreCase = true) ||
                (call.contactName?.contains(currentState.searchQuery, ignoreCase = true) == true)
            }
        }
        
        val searchFilteredSMSLogs = if (currentState.searchQuery.isEmpty()) {
            filteredSMSLogs
        } else {
            filteredSMSLogs.filter { sms ->
                sms.phoneNumber.contains(currentState.searchQuery, ignoreCase = true) ||
                (sms.contactName?.contains(currentState.searchQuery, ignoreCase = true) == true) ||
                sms.messageBody.contains(currentState.searchQuery, ignoreCase = true)
            }
        }
        
        _uiState.value = currentState.copy(
            conversations = searchFilteredConversations,
            callLogs = searchFilteredCallLogs,
            smsLogs = searchFilteredSMSLogs
        )
    }
    
    /**
     * Filter conversations by date
     */
    private fun filterByDate(conversations: List<ConversationItem>, dateFilter: DateFilter): List<ConversationItem> {
        if (dateFilter == DateFilter.ALL) return conversations
        
        val cutoffTime = getCutoffTime(dateFilter)
        return conversations.filter { it.lastMessageTime >= cutoffTime }
    }
    
    /**
     * Filter call logs by date
     */
    private fun filterCallLogsByDate(callLogs: List<CallHistoryItem>, dateFilter: DateFilter): List<CallHistoryItem> {
        if (dateFilter == DateFilter.ALL) return callLogs
        
        val cutoffTime = getCutoffTime(dateFilter)
        return callLogs.filter { it.timestamp >= cutoffTime }
    }
    
    /**
     * Filter SMS logs by date
     */
    private fun filterSMSLogsByDate(smsLogs: List<SMSHistoryItem>, dateFilter: DateFilter): List<SMSHistoryItem> {
        if (dateFilter == DateFilter.ALL) return smsLogs
        
        val cutoffTime = getCutoffTime(dateFilter)
        return smsLogs.filter { it.timestamp >= cutoffTime }
    }
    
    /**
     * Get cutoff timestamp for date filter
     */
    private fun getCutoffTime(dateFilter: DateFilter): Long {
        if (dateFilter == DateFilter.ALL) return 0L
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -dateFilter.days)
        return calendar.timeInMillis
    }
    
    /**
     * Get current UI state
     */
    private val currentState: ConversationHistoryState
        get() = _uiState.value
    
    /**
     * Refresh history from both local database and backend
     */
    fun refreshHistory() {
        loadHistory()
    }
    
    /**
     * Clear search and filters
     */
    fun clearFilters() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            searchQuery = "",
            dateFilter = DateFilter.ALL
        )
        applyFilters()
    }
}