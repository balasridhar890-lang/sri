package com.voiceassistant.android.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore preferences for app configuration
 */
private val Context.dataStore by preferencesDataStore("voice_assistant_prefs")

/**
 * Preferences keys
 */
object PrefKeys {
    // API Configuration
    val BACKEND_URL = stringPreferencesKey("backend_url")
    val USER_ID = intPreferencesKey("user_id")
    val API_KEY = stringPreferencesKey("api_key")
    val API_TOKEN = stringPreferencesKey("api_token")
    
    // Feature Toggles
    val AUTO_ANSWER_ENABLED = booleanPreferencesKey("auto_answer_enabled")
    val AUTO_REPLY_ENABLED = booleanPreferencesKey("auto_reply_enabled")
    val VOICE_ACTIVATION_ENABLED = booleanPreferencesKey("voice_activation_enabled")
    val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    val ACCESSIBILITY_ENABLED = booleanPreferencesKey("accessibility_enabled")
    
    // Voice Settings
    val WAKE_WORD_SENSITIVITY = floatPreferencesKey("wake_word_sensitivity")
    val VOICE_RESPONSE_VOLUME = floatPreferencesKey("voice_response_volume")
    val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
    val VOICE_GENDER = stringPreferencesKey("voice_gender")
    
    // Performance Settings
    val CALL_MONITORING_ENABLED = booleanPreferencesKey("call_monitoring_enabled")
    val SMS_MONITORING_ENABLED = booleanPreferencesKey("sms_monitoring_enabled")
    val CONTACT_SYNC_ENABLED = booleanPreferencesKey("contact_sync_enabled")
    val BATTERY_OPTIMIZATION_DISABLED = booleanPreferencesKey("battery_optimization_disabled")
    
    // Debug Settings
    val DEBUG_MODE_ENABLED = booleanPreferencesKey("debug_mode_enabled")
    val LOG_LEVEL = stringPreferencesKey("log_level")
    val PERFORMANCE_MONITORING_ENABLED = booleanPreferencesKey("performance_monitoring_enabled")
    
    // Statistics
    val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
    val TOTAL_CALLS_LOGGED = intPreferencesKey("total_calls_logged")
    val TOTAL_SMS_LOGGED = intPreferencesKey("total_sms_logged")
    val FIRST_RUN_COMPLETED = booleanPreferencesKey("first_run_completed")
}

/**
 * App preferences repository using DataStore
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    // API Configuration
    val backendUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[PrefKeys.BACKEND_URL] ?: "http://localhost:8000"
    }
    
    val userId: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PrefKeys.USER_ID] ?: -1
    }
    
    val apiKey: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PrefKeys.API_KEY]
    }
    
    val apiToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[PrefKeys.API_TOKEN]
    }
    
    // Feature Toggles
    val autoAnswerEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.AUTO_ANSWER_ENABLED] ?: true
    }
    
    val autoReplyEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.AUTO_REPLY_ENABLED] ?: true
    }
    
    val voiceActivationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.VOICE_ACTIVATION_ENABLED] ?: true
    }
    
    val syncEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.SYNC_ENABLED] ?: true
    }
    
    val darkModeEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.DARK_MODE_ENABLED] ?: false
    }
    
    val accessibilityEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.ACCESSIBILITY_ENABLED] ?: false
    }
    
    // Voice Settings
    val wakeWordSensitivity: Flow<Float> = dataStore.data.map { prefs ->
        prefs[PrefKeys.WAKE_WORD_SENSITIVITY] ?: 0.5f
    }
    
    val voiceResponseVolume: Flow<Float> = dataStore.data.map { prefs ->
        prefs[PrefKeys.VOICE_RESPONSE_VOLUME] ?: 0.8f
    }
    
    val voiceLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[PrefKeys.VOICE_LANGUAGE] ?: "en-US"
    }
    
    val voiceGender: Flow<String> = dataStore.data.map { prefs ->
        prefs[PrefKeys.VOICE_GENDER] ?: "neutral"
    }
    
    // Performance Settings
    val callMonitoringEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.CALL_MONITORING_ENABLED] ?: true
    }
    
    val smsMonitoringEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.SMS_MONITORING_ENABLED] ?: true
    }
    
    val contactSyncEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.CONTACT_SYNC_ENABLED] ?: true
    }
    
    val batteryOptimizationDisabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.BATTERY_OPTIMIZATION_DISABLED] ?: false
    }
    
    // Debug Settings
    val debugModeEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.DEBUG_MODE_ENABLED] ?: false
    }
    
    val logLevel: Flow<String> = dataStore.data.map { prefs ->
        prefs[PrefKeys.LOG_LEVEL] ?: "INFO"
    }
    
    val performanceMonitoringEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.PERFORMANCE_MONITORING_ENABLED] ?: false
    }
    
    // Statistics
    val lastBackupTimestamp: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PrefKeys.LAST_BACKUP_TIMESTAMP] ?: 0L
    }
    
    val totalCallsLogged: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PrefKeys.TOTAL_CALLS_LOGGED] ?: 0
    }
    
    val totalSmsLogged: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PrefKeys.TOTAL_SMS_LOGGED] ?: 0
    }
    
    val firstRunCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PrefKeys.FIRST_RUN_COMPLETED] ?: false
    }
    
    // Update methods
    suspend fun updateBackendUrl(url: String) {
        dataStore.edit { prefs -> prefs[PrefKeys.BACKEND_URL] = url }
    }
    
    suspend fun updateUserId(id: Int) {
        dataStore.edit { prefs -> prefs[PrefKeys.USER_ID] = id }
    }
    
    suspend fun updateApiKey(key: String?) {
        dataStore.edit { prefs -> 
            if (key != null) {
                prefs[PrefKeys.API_KEY] = key
            } else {
                prefs.remove(PrefKeys.API_KEY)
            }
        }
    }
    
    suspend fun updateApiToken(token: String?) {
        dataStore.edit { prefs -> 
            if (token != null) {
                prefs[PrefKeys.API_TOKEN] = token
            } else {
                prefs.remove(PrefKeys.API_TOKEN)
            }
        }
    }
    
    suspend fun updateAutoAnswerEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.AUTO_ANSWER_ENABLED] = enabled }
    }
    
    suspend fun updateAutoReplyEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.AUTO_REPLY_ENABLED] = enabled }
    }
    
    suspend fun updateVoiceActivationEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.VOICE_ACTIVATION_ENABLED] = enabled }
    }
    
    suspend fun updateSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.SYNC_ENABLED] = enabled }
    }
    
    suspend fun updateDarkModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.DARK_MODE_ENABLED] = enabled }
    }
    
    suspend fun updateAccessibilityEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.ACCESSIBILITY_ENABLED] = enabled }
    }
    
    suspend fun updateWakeWordSensitivity(sensitivity: Float) {
        dataStore.edit { prefs -> prefs[PrefKeys.WAKE_WORD_SENSITIVITY] = sensitivity }
    }
    
    suspend fun updateVoiceResponseVolume(volume: Float) {
        dataStore.edit { prefs -> prefs[PrefKeys.VOICE_RESPONSE_VOLUME] = volume }
    }
    
    suspend fun updateVoiceLanguage(language: String) {
        dataStore.edit { prefs -> prefs[PrefKeys.VOICE_LANGUAGE] = language }
    }
    
    suspend fun updateVoiceGender(gender: String) {
        dataStore.edit { prefs -> prefs[PrefKeys.VOICE_GENDER] = gender }
    }
    
    suspend fun updateCallMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.CALL_MONITORING_ENABLED] = enabled }
    }
    
    suspend fun updateSmsMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.SMS_MONITORING_ENABLED] = enabled }
    }
    
    suspend fun updateContactSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.CONTACT_SYNC_ENABLED] = enabled }
    }
    
    suspend fun updateBatteryOptimizationDisabled(disabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.BATTERY_OPTIMIZATION_DISABLED] = disabled }
    }
    
    suspend fun updateDebugModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.DEBUG_MODE_ENABLED] = enabled }
    }
    
    suspend fun updateLogLevel(level: String) {
        dataStore.edit { prefs -> prefs[PrefKeys.LOG_LEVEL] = level }
    }
    
    suspend fun updatePerformanceMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PrefKeys.PERFORMANCE_MONITORING_ENABLED] = enabled }
    }
    
    suspend fun updateLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { prefs -> prefs[PrefKeys.LAST_BACKUP_TIMESTAMP] = timestamp }
    }
    
    suspend fun incrementTotalCallsLogged() {
        dataStore.edit { prefs ->
            val current = prefs[PrefKeys.TOTAL_CALLS_LOGGED] ?: 0
            prefs[PrefKeys.TOTAL_CALLS_LOGGED] = current + 1
        }
    }
    
    suspend fun incrementTotalSmsLogged() {
        dataStore.edit { prefs ->
            val current = prefs[PrefKeys.TOTAL_SMS_LOGGED] ?: 0
            prefs[PrefKeys.TOTAL_SMS_LOGGED] = current + 1
        }
    }
    
    suspend fun setFirstRunCompleted() {
        dataStore.edit { prefs -> prefs[PrefKeys.FIRST_RUN_COMPLETED] = true }
    }
    
    suspend fun resetAll() {
        dataStore.edit { it.clear() }
    }
}

/**
 * Hilt module for providing DataStore preferences
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideAppPreferences(
        @ApplicationContext context: Context
    ): AppPreferences {
        return AppPreferences(context)
    }
}