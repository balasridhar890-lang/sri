package com.assistant.voicecore.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Boot receiver for automatic service startup after device reboot
 * Ensures voice service is resumed after system reboot
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var voiceServiceController: VoiceServiceController

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                handleLockedBootCompleted(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                handlePackageReplaced(context)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                val dataUri = intent.data
                if (dataUri?.scheme == "package" && 
                    dataUri.schemeSpecificPart == context.packageName) {
                    handlePackageReplaced(context)
                }
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        Timber.d("Boot completed - checking service auto-start preferences")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if user has enabled auto-start in preferences
                val autoStartEnabled = shouldAutoStartOnBoot(context)
                
                if (autoStartEnabled) {
                    Timber.d("Auto-start enabled, launching voice service")
                    startVoiceService(context)
                } else {
                    Timber.d("Auto-start disabled, not launching voice service")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle boot completed")
            }
        }
    }

    private fun handleLockedBootCompleted(context: Context) {
        Timber.d("Locked boot completed - voice service may not be available yet")
        
        // For locked boot, we might need to wait for the device to fully boot
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(30000) // Wait 30 seconds for device to fully boot
                handleBootCompleted(context)
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle locked boot completed")
            }
        }
    }

    private fun handlePackageReplaced(context: Context) {
        Timber.d("Package replaced - may need to restart services")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // On app update, restart voice service if it was running
                if (shouldAutoStartOnBoot(context)) {
                    Timber.d("App updated, restarting voice service")
                    startVoiceService(context)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle package replacement")
            }
        }
    }

    private fun shouldAutoStartOnBoot(context: Context): Boolean {
        return try {
            // In a real implementation, this would read from SharedPreferences
            // For now, we'll assume auto-start is enabled by default
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to read auto-start preference")
            false
        }
    }

    private fun startVoiceService(context: Context) {
        try {
            val serviceIntent = Intent(context, VoiceCoreService::class.java).apply {
                action = VoiceCoreService.ACTION_START
            }
            
            // For Android 8.0+ (API 26+), we need to use startForegroundService
            // and then call startForeground within the service
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Timber.d("Voice service start command sent")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to start voice service")
        }
    }
}

/**
 * Voice service controller for managing service lifecycle
 */
class VoiceServiceController {
    
    fun startVoiceService(context: Context) {
        val intent = Intent(context, VoiceCoreService::class.java).apply {
            action = VoiceCoreService.ACTION_START
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    fun stopVoiceService(context: Context) {
        val intent = Intent(context, VoiceCoreService::class.java).apply {
            action = VoiceCoreService.ACTION_STOP
        }
        context.startService(intent)
    }
    
    fun pauseVoiceService(context: Context) {
        val intent = Intent(context, VoiceCoreService::class.java).apply {
            action = VoiceCoreService.ACTION_PAUSE
        }
        context.startService(intent)
    }
    
    fun resumeVoiceService(context: Context) {
        val intent = Intent(context, VoiceCoreService::class.java).apply {
            action = VoiceCoreService.ACTION_RESUME
        }
        context.startService(intent)
    }
}