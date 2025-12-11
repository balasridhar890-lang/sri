package com.assistant.voicecore.viewmodel

import android.content.Context
import android.content.Intent
import android.media.*
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.assistant.voicecore.VoiceCoreApplication
import com.assistant.voicecore.model.*
import com.assistant.voicecore.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for managing voice service state and operations
 */
@HiltViewModel
class VoiceServiceViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var audioManager: AudioManager? = null
    private var audioRecord: AudioRecord? = null

    // State management
    private val _serviceStatus = MutableStateFlow(VoiceServiceStatus())
    val serviceStatus: StateFlow<VoiceServiceStatus> = _serviceStatus.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Constants
    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
        private const val WAKE_WORD = "hey jarvis"
    }

    /**
     * Initialize speech recognition
     */
    suspend fun initializeSpeechRecognition() {
        withContext(Dispatchers.Main) {
            try {
                updateServiceStatus(VoiceServiceState.INITIALIZING)

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(VoiceCoreApplication.instance)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Timber.d("Ready for speech")
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        Timber.d("Beginning of speech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Update audio level indicator
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Process audio buffer for wake word detection
                        buffer?.let { processAudioBuffer(it) }
                    }

                    override fun onEndOfSpeech() {
                        Timber.d("End of speech")
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        Timber.e("Speech recognition error: $error")
                        handleSpeechError(error)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.let {
                            val transcript = it[0] ?: ""
                            Timber.d("Speech recognition results: $transcript")
                            _currentTranscript.value = transcript
                            
                            if (isWakeWord(transcript)) {
                                onWakeWordDetected()
                            } else {
                                processSpeechInput(transcript)
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.let {
                            _currentTranscript.value = it[0] ?: ""
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Handle special events
                    }
                })

                updateServiceStatus(VoiceServiceState.LISTENING)
                Timber.d("Speech recognition initialized successfully")

            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize speech recognition")
                updateServiceStatus(VoiceServiceState.ERROR, e.message)
                throw e
            }
        }
    }

    /**
     * Initialize text-to-speech
     */
    suspend fun initializeTextToSpeech() {
        withContext(Dispatchers.Main) {
            try {
                updateServiceStatus(VoiceServiceState.INITIALIZING)

                textToSpeech = TextToSpeech(VoiceCoreApplication.instance) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val locale = Locale.getDefault()
                        val result = textToSpeech?.setLanguage(locale)
                        
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Timber.w("TTS language not supported: $locale")
                        }
                        
                        updateServiceStatus(VoiceServiceState.LISTENING)
                        Timber.d("Text-to-speech initialized successfully")
                    } else {
                        throw Exception("TTS initialization failed with status: $status")
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize text-to-speech")
                updateServiceStatus(VoiceServiceState.ERROR, e.message)
                throw e
            }
        }
    }

    /**
     * Start wake word detection
     */
    suspend fun startWakeWordDetection() {
        withContext(Dispatchers.Main) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 500)
                }

                speechRecognizer?.startListening(intent)
                updateServiceStatus(VoiceServiceState.LISTENING)
                Timber.d("Wake word detection started")

            } catch (e: Exception) {
                Timber.e(e, "Failed to start wake word detection")
                updateServiceStatus(VoiceServiceState.ERROR, e.message)
                throw e
            }
        }
    }

    /**
     * Start recording audio
     */
    suspend fun startRecording() {
        withContext(Dispatchers.Main) {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                ) * BUFFER_SIZE_FACTOR

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                audioRecord?.startRecording()
                _isRecording.value = true
                
                // Start reading audio data in background
                startAudioReading()
                
                Timber.d("Audio recording started")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start recording")
                throw e
            }
        }
    }

    /**
     * Stop recording audio
     */
    suspend fun stopRecording() {
        withContext(Dispatchers.Main) {
            try {
                _isRecording.value = false
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                
                Timber.d("Audio recording stopped")
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop recording")
            }
        }
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = _isRecording.value

    /**
     * Process speech input after wake word detection
     */
    private suspend fun processSpeechInput(transcript: String) {
        if (transcript.isEmpty()) return

        viewModelScope.launch {
            try {
                updateServiceStatus(VoiceServiceState.PROCESSING)
                
                // Process with backend
                val response = processConversation(transcript)
                
                // Speak the response
                speakText(response)
                
                // Update status back to listening
                updateServiceStatus(VoiceServiceState.LISTENING)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to process speech input")
                updateServiceStatus(VoiceServiceState.ERROR, e.message)
            }
        }
    }

    /**
     * Transcribe audio data to text
     */
    suspend fun transcribeAudio(audioData: ByteArray): String {
        return withContext(Dispatchers.IO) {
            try {
                // For demo purposes, return the current transcript
                // In a real implementation, you'd send this to Google Speech-to-Text API
                _currentTranscript.value
            } catch (e: Exception) {
                Timber.e(e, "Audio transcription failed")
                ""
            }
        }
    }

    /**
     * Process conversation with backend API
     */
    suspend fun processConversation(text: String): String {
        return withContext(Dispatchers.IO) {
            try {
                updateServiceStatus(VoiceServiceState.PROCESSING)
                
                val request = ConversationRequest(userId = 1, text = text) // Default user ID
                val response = conversationRepository.processConversation(request)
                
                if (response.isSuccessful) {
                    val conversationResponse = response.body()
                    conversationResponse?.gptResponse ?: "I'm sorry, I didn't understand that."
                } else {
                    Timber.e("Backend API error: ${response.code()}")
                    "I'm sorry, I'm having trouble connecting right now."
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Conversation processing failed")
                "I'm sorry, I encountered an error. Please try again."
            }
        }
    }

    /**
     * Speak text using TTS
     */
    suspend fun speakText(text: String) {
        withContext(Dispatchers.Main) {
            try {
                updateServiceStatus(VoiceServiceState.SPEAKING)
                
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
                
                // Wait for speech to complete
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Timber.d("TTS started: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Timber.d("TTS completed: $utteranceId")
                        updateServiceStatus(VoiceServiceState.LISTENING)
                    }

                    override fun onError(utteranceId: String?) {
                        Timber.e("TTS error: $utteranceId")
                        updateServiceStatus(VoiceServiceState.ERROR, "TTS error")
                    }
                })
                
            } catch (e: Exception) {
                Timber.e(e, "Text-to-speech failed")
                updateServiceStatus(VoiceServiceState.ERROR, e.message)
            }
        }
    }

    /**
     * Play audio (placeholder for audio playback implementation)
     */
    suspend fun playAudio() {
        withContext(Dispatchers.Main) {
            try {
                // In a real implementation, you would play the synthesized audio
                // through the call audio stream or phone speaker
                Timber.d("Playing audio response")
            } catch (e: Exception) {
                Timber.e(e, "Audio playback failed")
            }
        }
    }

    /**
     * Pause listening
     */
    suspend fun pauseListening() {
        withContext(Dispatchers.Main) {
            speechRecognizer?.stopListening()
            _isListening.value = false
            updateServiceStatus(VoiceServiceState.IDLE)
        }
    }

    /**
     * Resume listening
     */
    suspend fun resumeListening() {
        startWakeWordDetection()
    }

    /**
     * Start voice service
     */
    suspend fun startVoiceService() {
        withContext(Dispatchers.Main) {
            try {
                updateServiceStatus(VoiceServiceState.INITIALIZING)
                
                initializeSpeechRecognition()
                initializeTextToSpeech()
                startWakeWordDetection()
                
                _serviceStatus.value = _serviceStatus.value.copy(
                    isRunning = true,
                    foregroundServiceActive = true
                )
                
                Timber.d("Voice service started successfully")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to start voice service")
                updateServiceStatus(VoiceServiceState.ERROR, e.message)
            }
        }
    }

    /**
     * Stop voice service
     */
    suspend fun stopVoiceService() {
        withContext(Dispatchers.Main) {
            try {
                speechRecognizer?.destroy()
                textToSpeech?.shutdown()
                audioRecord?.release()
                
                _serviceStatus.value = _serviceStatus.value.copy(
                    isRunning = false,
                    isListening = false,
                    foregroundServiceActive = false
                )
                
                _isListening.value = false
                _isRecording.value = false
                
                updateServiceStatus(VoiceServiceState.IDLE)
                
                Timber.d("Voice service stopped")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop voice service")
            }
        }
    }

    /**
     * Update service status
     */
    fun updateServiceStatus(state: VoiceServiceState, error: String? = null) {
        _serviceStatus.value = _serviceStatus.value.copy(
            currentState = state,
            lastActivity = java.util.Date(),
            errorMessage = error
        )
        _errorMessage.value = error
    }

    /**
     * Process audio buffer for wake word detection
     */
    private fun processAudioBuffer(buffer: ByteArray) {
        viewModelScope.launch {
            try {
                // Simple wake word detection (in a real app, you'd use a proper wake word engine)
                // This is a placeholder implementation
                val audioData = AudioData(buffer, SAMPLE_RATE, 1)
                // Process with wake word detection algorithm
                
            } catch (e: Exception) {
                Timber.e(e, "Audio buffer processing failed")
            }
        }
    }

    /**
     * Check if transcript contains wake word
     */
    private fun isWakeWord(transcript: String): Boolean {
        return transcript.trim().lowercase().contains(WAKE_WORD)
    }

    /**
     * Handle wake word detection
     */
    private fun onWakeWordDetected() {
        viewModelScope.launch {
            try {
                Timber.d("Wake word detected: Hey Jarvis")
                updateServiceStatus(VoiceServiceState.PROCESSING)
                
                // Start recording for user speech
                startRecording()
                
                // Auto-stop recording after timeout
                viewModelScope.launch {
                    delay(5000) // 5 second recording window
                    if (isRecording()) {
                        stopRecording()
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Wake word handling failed")
                updateServiceStatus(VoiceServiceState.ERROR, e.message)
            }
        }
    }

    /**
     * Start reading audio data
     */
    private fun startAudioReading() {
        viewModelScope.launch {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                ) * BUFFER_SIZE_FACTOR
                
                val buffer = ShortArray(bufferSize / 2)
                
                while (_isRecording.value) {
                    val result = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (result > 0) {
                        // Process audio data
                        val audioData = buffer.toByteArray()
                        processAudioBuffer(audioData)
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Audio reading failed")
            }
        }
    }

    /**
     * Handle speech recognition errors
     */
    private fun handleSpeechError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error: $error"
        }
        
        Timber.e("Speech recognition error: $errorMessage")
        updateServiceStatus(VoiceServiceState.ERROR, errorMessage)
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        audioRecord?.release()
    }
}