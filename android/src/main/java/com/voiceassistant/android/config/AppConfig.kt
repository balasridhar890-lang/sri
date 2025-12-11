package com.voiceassistant.android.config

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application configuration and preferences
 */
@Singleton
class AppConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "voice_assistant_prefs"
        private const val BACKEND_URL_KEY = "backend_url"
        private const val USER_ID_KEY = "user_id"
        private const val AUTO_ANSWER_ENABLED_KEY = "auto_answer_enabled"
        private const val AUTO_REPLY_ENABLED_KEY = "auto_reply_enabled"
        private const val SYNC_ENABLED_KEY = "sync_enabled"
        
        // Default values
        private const val DEFAULT_BACKEND_URL = "http://localhost:8000"
        private const val DEFAULT_USER_ID = -1
        private const val DEFAULT_AUTO_ANSWER_ENABLED = true
        private const val DEFAULT_AUTO_REPLY_ENABLED = true
        private const val DEFAULT_SYNC_ENABLED = true
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    var backendUrl: String
        get() = prefs.getString(BACKEND_URL_KEY, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
        set(value) = prefs.edit().putString(BACKEND_URL_KEY, value).apply()
    
    var userId: Int
        get() = prefs.getInt(USER_ID_KEY, DEFAULT_USER_ID)
        set(value) = prefs.edit().putInt(USER_ID_KEY, value).apply()
    
    var autoAnswerEnabled: Boolean
        get() = prefs.getBoolean(AUTO_ANSWER_ENABLED_KEY, DEFAULT_AUTO_ANSWER_ENABLED)
        set(value) = prefs.edit().putBoolean(AUTO_ANSWER_ENABLED_KEY, value).apply()
    
    var autoReplyEnabled: Boolean
        get() = prefs.getBoolean(AUTO_REPLY_ENABLED_KEY, DEFAULT_AUTO_REPLY_ENABLED)
        set(value) = prefs.edit().putBoolean(AUTO_REPLY_ENABLED_KEY, value).apply()
    
    var syncEnabled: Boolean
        get() = prefs.getBoolean(SYNC_ENABLED_KEY, DEFAULT_SYNC_ENABLED)
        set(value) = prefs.edit().putBoolean(SYNC_ENABLED_KEY, value).apply()
    
    fun reset() {
        prefs.edit().clear().apply()
    }
}
