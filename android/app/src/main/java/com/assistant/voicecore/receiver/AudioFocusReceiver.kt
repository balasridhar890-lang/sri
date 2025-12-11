package com.assistant.voicecore.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Audio focus receiver for handling audio events
 * Manages audio focus changes and device audio state changes
 */
@AndroidEntryPoint
class AudioFocusReceiver : BroadcastReceiver() {

    @Inject
    lateinit var audioManager: AudioManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AudioManager.ACTION_AUDIO_FOCUS_CHANGED -> {
                handleAudioFocusChange(intent)
            }
            AudioManager.ACTION_HEADSET_PLUG -> {
                handleHeadsetPlug(intent)
            }
            "android.media.AUDIO_BECOMING_NOISY" -> {
                handleAudioBecomingNoisy()
            }
        }
    }

    private fun handleAudioFocusChange(intent: Intent) {
        val focusChange = intent.getIntExtra(AudioManager.EXTRA_AUDIO_FOCUS_CHANGE, -1)
        
        when (focusChange) {
            AudioManager.AUDIO_FOCUS_GAIN -> {
                Timber.d("Audio focus gained")
                // Resume normal audio processing
                resumeAudioProcessing()
            }
            AudioManager.AUDIO_FOCUS_LOSS -> {
                Timber.d("Audio focus lost")
                // Stop audio processing and release resources
                pauseAudioProcessing()
            }
            AudioManager.AUDIO_FOCUS_LOSS_TRANSIENT -> {
                Timber.d("Audio focus lost transient")
                // Pause audio processing temporarily
                pauseAudioProcessing()
            }
            AudioManager.AUDIO_FOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Timber.d("Audio focus loss transient - duck")
                // Reduce volume but continue processing
                duckAudioVolume()
            }
        }
    }

    private fun handleHeadsetPlug(intent: Intent) {
        val state = intent.getIntExtra("state", -1)
        
        when (state) {
            0 -> {
                Timber.d("Headset unplugged")
                // Switch to speaker output
                switchToSpeakerOutput()
            }
            1 -> {
                Timber.d("Headset plugged")
                // Switch to headset output
                switchToHeadsetOutput()
            }
        }
    }

    private fun handleAudioBecomingNoisy() {
        Timber.d("Audio becoming noisy")
        // Handle audio becoming noisy (e.g., headphones unplugged during playback)
        pauseAudioProcessing()
    }

    private fun resumeAudioProcessing() {
        // Resume voice processing and TTS
        // This would interact with the VoiceService to resume operations
        Timber.d("Resuming audio processing")
    }

    private fun pauseAudioProcessing() {
        // Pause voice processing and stop TTS
        // This would interact with the VoiceService to pause operations
        Timber.d("Pausing audio processing")
    }

    private fun duckAudioVolume() {
        // Reduce audio volume but continue processing
        // This is typically used for navigation apps
        Timber.d("Ducking audio volume")
    }

    private fun switchToSpeakerOutput() {
        // Switch audio output to device speaker
        Timber.d("Switching to speaker output")
    }

    private fun switchToHeadsetOutput() {
        // Switch audio output to connected headset
        Timber.d("Switching to headset output")
    }
}