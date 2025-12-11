package com.assistant.voicecore

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Main Application class for VoiceCore assistant
 * Initializes global components like dependency injection, logging, and notification channels
 */
@HiltAndroidApp
class VoiceCoreApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    companion object {
        const val CHANNEL_VOICE_SERVICE = "voice_service"
        const val CHANNEL_CALL_SERVICE = "call_service"
        const val CHANNEL_SYSTEM_ALERTS = "system_alerts"
        
        lateinit var instance: VoiceCoreApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Create notification channels
        createNotificationChannels()

        // Initialize any other components
        initializeComponents()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Voice Service Channel
            val voiceChannel = NotificationChannel(
                CHANNEL_VOICE_SERVICE,
                "Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background voice processing and wake word detection"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            // Call Service Channel
            val callChannel = NotificationChannel(
                CHANNEL_CALL_SERVICE,
                "Call Management",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Automatic call answering and management"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }

            // System Alerts Channel
            val alertsChannel = NotificationChannel(
                CHANNEL_SYSTEM_ALERTS,
                "System Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Important system notifications and alerts"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(voiceChannel)
            notificationManager.createNotificationChannel(callChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    private fun initializeComponents() {
        Timber.d("Initializing VoiceCore components")
        // Initialize any app-wide components here
        // This is where you'd initialize shared preferences, database, etc.
    }
}