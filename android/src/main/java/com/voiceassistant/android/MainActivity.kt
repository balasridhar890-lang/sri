package com.voiceassistant.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.voiceassistant.android.config.AppConfig
import com.voiceassistant.android.permissions.PermissionManager
import com.voiceassistant.android.services.phone.PhoneStateService
import com.voiceassistant.android.services.sync.SyncService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity with permission handling and service initialization
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    @Inject
    lateinit var permissionManager: PermissionManager
    
    @Inject
    lateinit var appConfig: AppConfig
    
    private var permissionRequestCount = 0
    private var deniedPermissions = mutableSetOf<String>()
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        Log.d(TAG, "Permission request result: $result")
        
        val deniedList = result.filter { !it.value }.keys
        if (deniedList.isNotEmpty()) {
            deniedPermissions.addAll(deniedList)
            Log.w(TAG, "Permissions denied: $deniedList")
            
            // Show fallback message
            showPermissionDeniedFallback(deniedList.toList())
        } else {
            deniedPermissions.clear()
            Log.d(TAG, "All permissions granted")
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
            startServices()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Install splash screen
        installSplashScreen()
        
        Log.d(TAG, "MainActivity created")
        
        // Check and request permissions
        checkAndRequestPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check critical permissions are still granted
        if (!permissionManager.hasCriticalPermissions()) {
            Log.w(TAG, "Critical permissions lost, re-requesting")
            checkAndRequestPermissions()
        }
    }
    
    private fun checkAndRequestPermissions() {
        Log.d(TAG, "Checking permissions")
        
        val missingPermissions = permissionManager.getMissingPermissions(
            PermissionManager.ALL_REQUIRED_PERMISSIONS
        )
        
        if (missingPermissions.isEmpty()) {
            Log.d(TAG, "All permissions already granted")
            startServices()
            return
        }
        
        Log.d(TAG, "Missing permissions: ${missingPermissions.toList()}")
        
        // Show explanation dialog before requesting
        if (permissionRequestCount == 0) {
            showPermissionExplanationDialog(missingPermissions)
        } else {
            requestPermissions(missingPermissions)
        }
        
        permissionRequestCount++
    }
    
    private fun showPermissionExplanationDialog(permissions: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle("App Permissions Required")
            .setMessage(buildPermissionMessage(permissions))
            .setPositiveButton("Grant") { _, _ ->
                requestPermissions(permissions)
            }
            .setNegativeButton("Cancel") { _, _ ->
                showFallbackOptions()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun requestPermissions(permissions: Array<String>) {
        Log.d(TAG, "Requesting permissions: ${permissions.toList()}")
        
        val toRequest = permissions.filter {
            permissionManager.shouldRequestPermission(this, it)
        }.toTypedArray()
        
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest)
        } else {
            startServices()
        }
    }
    
    private fun showPermissionDeniedFallback(deniedPermissions: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("Permissions Denied")
            .setMessage(buildDeniedMessage(deniedPermissions))
            .setPositiveButton("Continue Anyway") { _, _ ->
                // Proceed with limited functionality
                startServicesWithFallback()
            }
            .setNegativeButton("Quit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showFallbackOptions() {
        AlertDialog.Builder(this)
            .setTitle("Limited Functionality")
            .setMessage(
                "Without required permissions, the app will have limited functionality. " +
                "Continue with fallback mode?"
            )
            .setPositiveButton("Continue") { _, _ ->
                startServicesWithFallback()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun startServices() {
        Log.d(TAG, "Starting services")
        
        // Start phone state monitoring service
        val phoneStateIntent = Intent(this, PhoneStateService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(phoneStateIntent)
        } else {
            startService(phoneStateIntent)
        }
        
        // Start sync service
        val syncIntent = Intent(this, SyncService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(syncIntent)
        } else {
            startService(syncIntent)
        }
        
        Toast.makeText(this, "Services started", Toast.LENGTH_SHORT).show()
    }
    
    private fun startServicesWithFallback() {
        Log.d(TAG, "Starting services with fallback")
        
        // Only start services for which we have required permissions
        if (permissionManager.hasCallPermissions()) {
            val phoneStateIntent = Intent(this, PhoneStateService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(phoneStateIntent)
            } else {
                startService(phoneStateIntent)
            }
        }
        
        Toast.makeText(
            this,
            "Services started with limited functionality",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun buildPermissionMessage(permissions: Array<String>): String {
        val messages = mutableListOf<String>()
        
        if (permissions.contains(Manifest.permission.READ_PHONE_STATE)) {
            messages.add("• Phone State: To monitor calls and auto-answer")
        }
        if (permissions.contains(Manifest.permission.RECEIVE_SMS)) {
            messages.add("• SMS: To receive and auto-reply to messages")
        }
        if (permissions.contains(Manifest.permission.READ_CONTACTS)) {
            messages.add("• Contacts: To access your contact list")
        }
        if (permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            messages.add("• Storage: To log and store history")
        }
        
        return messages.joinToString("\n")
    }
    
    private fun buildDeniedMessage(deniedPermissions: List<String>): String {
        val messages = mutableListOf<String>()
        
        deniedPermissions.forEach { permission ->
            when (permission) {
                Manifest.permission.READ_PHONE_STATE -> {
                    messages.add("• Phone State (Call monitoring disabled)")
                }
                Manifest.permission.RECEIVE_SMS -> {
                    messages.add("• SMS (Auto-reply disabled)")
                }
                Manifest.permission.READ_CONTACTS -> {
                    messages.add("• Contacts (Limited access)")
                }
                Manifest.permission.READ_EXTERNAL_STORAGE -> {
                    messages.add("• Storage (History logging limited)")
                }
            }
        }
        
        return "The following permissions were denied:\n\n${messages.joinToString("\n")}\n\n" +
                "You can still use the app with limited functionality."
    }
}
