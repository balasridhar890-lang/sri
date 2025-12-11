package com.assistant.voicecore.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.assistant.voicecore.R
import com.assistant.voicecore.VoiceCoreApplication.Companion.CHANNEL_VOICE_SERVICE
import com.assistant.voicecore.ui.MainActivity
import com.assistant.voicecore.viewmodel.VoiceServiceViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service for voice processing and wake word detection
 * Manages the complete voice pipeline: wake word → speech recognition → backend → TTS → playback
 */
@AndroidEntryPoint
class VoiceCoreService : LifecycleService() {

    @Inject
    lateinit var voiceServiceViewModel: VoiceServiceViewModel

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isInitialized = false
    private var isProcessing = false

    private val binder = VoiceServiceBinder()

    companion object {
        const val ACTION_START = "com.assistant.voicecore.action.START"
        const val ACTION_STOP = "com.assistant.voicecore.action.STOP"
        const val ACTION_PAUSE = "com.assistant.voicecore.action.PAUSE"
        const val ACTION_RESUME = "com.assistant.voicecore.action.RESUME"
        
        const val NOTIFICATION_ID = 1001
        const val FOREGROUND_SERVICE_TYPE = "android.app.FOREGROUND_SERVICE_TYPE_MICROPHONE"
    }

    inner class VoiceServiceBinder : Binder() {
        fun getService(): VoiceCoreService = this@VoiceCoreService
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("VoiceCoreService onCreate")
        initializeService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_PAUSE -> {
                pauseService()
            }
            ACTION_RESUME -> {
                resumeService()
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Timber.d("VoiceCoreService destroyed")
    }

    private fun initializeService() {
        if (isInitialized) return
        
        serviceScope.launch {
            try {
                Timber.d("Initializing voice service...")
                
                // Request battery optimization exemption
                requestBatteryOptimizationExemption()
                
                // Initialize speech recognition
                initializeSpeechRecognition()
                
                // Initialize text-to-speech
                initializeTextToSpeech()
                
                // Start wake word detection
                startWakeWordDetection()
                
                isInitialized = true
                updateServiceStatus(VoiceServiceState.LISTENING)
                
                Timber.d("Voice service initialization complete")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize voice service")
                updateServiceStatus(VoiceServiceState.ERROR, e.message)
            }
        }
    }

    private suspend fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = android.net.Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                Timber.d("Requested battery optimization exemption")
            }
        }
    }

    private suspend fun initializeSpeechRecognition() {
        try {
            voiceServiceViewModel.initializeSpeechRecognition()
            Timber.d("Speech recognition initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize speech recognition")
            throw e
        }
    }

    private suspend fun initializeTextToSpeech() {
        try {
            voiceServiceViewModel.initializeTextToSpeech()
            Timber.d("Text-to-speech initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize text-to-speech")
            throw e
        }
    }

    private suspend fun startWakeWordDetection() {
        try {
            voiceServiceViewModel.startWakeWordDetection()
            Timber.d("Wake word detection started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start wake word detection")
            throw e
        }
    }

    private fun startForegroundService() {
        if (!isInitialized) {
            initializeService()
        }

        val notification = createServiceNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        Timber.d("Voice service started as foreground service")
    }

    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_VOICE_SERVICE)
            .setContentTitle("VoiceCore Assistant")
            .setContentText("Listening for \"Hey Jarvis\"")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun pauseService() {
        serviceScope.launch {
            try {
                voiceServiceViewModel.pauseListening()
                updateServiceStatus(VoiceServiceState.IDLE)
                Timber.d("Voice service paused")
            } catch (e: Exception) {
                Timber.e(e, "Failed to pause service")
            }
        }
    }

    private fun resumeService() {
        serviceScope.launch {
            try {
                voiceServiceViewModel.resumeListening()
                updateServiceStatus(VoiceServiceState.LISTENING)
                Timber.d("Voice service resumed")
            } catch (e: Exception) {
                Timber.e(e, "Failed to resume service")
            }
        }
    }

    private fun updateServiceStatus(state: VoiceServiceState, error: String? = null) {
        voiceServiceViewModel.updateServiceStatus(state, error)
        
        // Update notification if running in foreground
        if (isInitialized && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, createServiceNotification())
        }
    }

    /**
     * Process voice input pipeline
     */
    fun processVoiceInput(audioData: ByteArray) {
        if (isProcessing) return
        
        isProcessing = true
        serviceScope.launch {
            try {
                updateServiceStatus(VoiceServiceState.PROCESSING)
                
                // 1. Speech-to-Text
                val transcript = voiceServiceViewModel.transcribeAudio(audioData)
                Timber.d("Transcribed: $transcript")
                
                if (transcript.isNotEmpty()) {
                    // 2. Process with backend
                    val response = voiceServiceViewModel.processConversation(transcript)
                    Timber.d("Backend response: $response")
                    
                    // 3. Text-to-Speech
                    voiceServiceViewModel.speakText(response)
                    
                    // 4. Play audio
                    voiceServiceViewModel.playAudio()
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Voice processing failed")
                updateServiceStatus(VoiceServiceState.ERROR, e.message)
            } finally {
                isProcessing = false
                updateServiceStatus(VoiceServiceState.LISTENING)
            }
        }
    }

    /**
     * Handle wake word detection
     */
    fun onWakeWordDetected() {
        Timber.d("Wake word detected: Hey Jarvis")
        serviceScope.launch {
            try {
                updateServiceState(VoiceServiceState.PROCESSING)
                
                // Start recording after wake word
                voiceServiceViewModel.startRecording()
                
                // Auto-stop recording after timeout
                delay(5000) // 5 second recording window
                if (voiceServiceViewModel.isRecording()) {
                    voiceServiceViewModel.stopRecording()
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Wake word handling failed")
                updateServiceStatus(VoiceServiceState.ERROR, e.message)
            }
        }
    }

    private suspend fun updateServiceState(state: VoiceServiceState) {
        updateServiceStatus(state)
    }
}