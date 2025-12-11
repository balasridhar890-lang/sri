package com.voiceassistant.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class with Hilt and notification channel initialization
 */
@HiltAndroidApp
class VoiceAssistantApp : Application() {
    companion object {
        private const val TAG = "VoiceAssistantApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created")
        
        // Create notification channels for foreground services
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Phone state service channel
            val phoneStateChannel = NotificationChannel(
                "phone_state_channel",
                "Phone State Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for phone state monitoring"
                enableVibration(false)
                enableLights(false)
            }
            
            // SMS service channel
            val smsChannel = NotificationChannel(
                "sms_channel",
                "SMS Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for SMS auto-reply"
            }
            
            // Sync service channel
            val syncChannel = NotificationChannel(
                "sync_channel",
                "Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for data synchronization"
                enableVibration(false)
                enableLights(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannels(
                listOf(phoneStateChannel, smsChannel, syncChannel)
            )
            
            Log.d(TAG, "Notification channels created")
        }
    }
}
