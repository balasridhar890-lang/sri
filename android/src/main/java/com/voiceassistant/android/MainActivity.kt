package com.voiceassistant.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceassistant.android.ui.theme.AppTheme
import com.voiceassistant.android.ui.AppNavigation
import com.voiceassistant.android.config.AppConfig
import com.voiceassistant.android.permissions.PermissionManager
import com.voiceassistant.android.services.phone.PhoneStateService
import com.voiceassistant.android.services.sync.SyncService
import com.voiceassistant.android.viewmodel.MainActivityViewModel
import com.voiceassistant.android.viewmodel.MainActivityState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity with Compose UI and permission handling
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    @Inject
    lateinit var permissionManager: PermissionManager
    
    @Inject
    lateinit var appConfig: AppConfig
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        Log.d(TAG, "Permission request result: $result")
        
        val deniedList = result.filter { !it.value }.keys
        if (deniedList.isNotEmpty()) {
            Log.w(TAG, "Permissions denied: $deniedList")
            // Handle denied permissions
        } else {
            Log.d(TAG, "All permissions granted")
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
            // Start services after permissions granted
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Install splash screen
        installSplashScreen()
        
        Log.d(TAG, "MainActivity created")
        
        setContent {
            AppTheme {
                val viewModel: MainActivityViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                
                MainActivityContent(
                    uiState = uiState,
                    onCheckPermissions = viewModel::checkPermissions,
                    onRequestPermissions = viewModel::requestPermissions,
                    onStartServices = viewModel::startServices,
                    onDismissPermissionDialog = viewModel::dismissPermissionDialog,
                    permissionLauncher = permissionLauncher
                )
            }
        }
    }
}

@Composable
fun MainActivityContent(
    uiState: MainActivityState,
    onCheckPermissions: () -> Unit,
    onRequestPermissions: (Array<String>) -> Unit,
    onStartServices: () -> Unit,
    onDismissPermissionDialog: () -> Unit,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    // Check permissions on start
    LaunchedEffect(Unit) {
        onCheckPermissions()
    }
    
    // Handle permission request
    LaunchedEffect(uiState.permissionRequestPending) {
        if (uiState.permissionRequestPending) {
            val permissions = uiState.missingPermissions
            if (permissions.isNotEmpty()) {
                permissionLauncher.launch(permissions)
            }
            onDismissPermissionDialog()
        }
    }
    
    // Start services when ready
    LaunchedEffect(uiState.permissionsGranted, uiState.servicesStarted) {
        if (uiState.permissionsGranted && !uiState.servicesStarted) {
            onStartServices()
        }
    }
    
    when {
        uiState.showPermissionDialog -> {
            PermissionRequestDialog(
                missingPermissions = uiState.missingPermissions,
                onGrantPermissions = { onRequestPermissions(uiState.missingPermissions) },
                onDismiss = onDismissPermissionDialog
            )
        }
        
        uiState.servicesStarted -> {
            // Main app navigation
            AppNavigation()
        }
        
        uiState.isInitializing -> {
            // Show loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Initializing Voice Assistant...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        else -> {
            // Default state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun PermissionRequestDialog(
    missingPermissions: Array<String>,
    onGrantPermissions: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Permissions Required")
        },
        text = {
            Column {
                Text("To provide the best experience, this app needs the following permissions:")
                Spacer(modifier = Modifier.height(8.dp))
                missingPermissions.forEach { permission ->
                    val description = when (permission) {
                        Manifest.permission.READ_PHONE_STATE -> "• Monitor calls and auto-answer"
                        Manifest.permission.RECEIVE_SMS -> "• Receive and auto-reply to messages"
                        Manifest.permission.READ_CONTACTS -> "• Access your contact list"
                        Manifest.permission.READ_EXTERNAL_STORAGE -> "• Log and store history"
                        Manifest.permission.CALL_PHONE -> "• Make phone calls"
                        Manifest.permission.SEND_SMS -> "• Send SMS messages"
                        else -> "• $permission"
                    }
                    Text(text = description)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onGrantPermissions) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
