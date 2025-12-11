package com.assistant.voicecore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.assistant.voicecore.model.ConversationLog
import com.assistant.voicecore.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel for managing application-wide state and data
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _recentConversations = MutableStateFlow<List<ConversationLog>>(emptyList())
    val recentConversations: StateFlow<List<ConversationLog>> = _recentConversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Load recent conversations
     */
    fun loadRecentConversations(limit: Int = 10) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // In a real implementation, this would fetch from local database
                // For now, we'll return an empty list or mock data
                val conversations = emptyList<ConversationLog>()
                _recentConversations.value = conversations
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load conversations: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}