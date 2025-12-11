package com.assistant.voicecore.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Listen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.assistant.voicecore.R
import com.assistant.voicecore.ui.components.PermissionCard
import com.assistant.voicecore.ui.components.ServiceStatusCard
import com.assistant.voicecore.ui.components.WaveformVisualizer
import com.assistant.voicecore.viewmodel.MainViewModel
import com.assistant.voicecore.viewmodel.VoiceServiceViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for VoiceCore Android application
 * Provides the primary UI for voice service control and status monitoring
 */
@AndroidEntryPoint
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainActivity() {
    val mainViewModel: MainViewModel = hiltViewModel()
    val voiceServiceViewModel: VoiceServiceViewModel = hiltViewModel()
    
    val context = LocalContext.current
    
    // Permission states
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val phonePermissionState = rememberPermissionState(Manifest.permission.CALL_PHONE)
    val phoneStatePermissionState = rememberPermissionState(Manifest.permission.READ_PHONE_STATE)
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val overlayPermissionState = rememberPermissionState(Manifest.permission.SYSTEM_ALERT_WINDOW)
    
    // UI State
    val serviceStatus by voiceServiceViewModel.serviceStatus.collectAsState()
    val isListening by voiceServiceViewModel.isListening.collectAsState()
    val currentTranscript by voiceServiceViewModel.currentTranscript.collectAsState()
    val recentConversations by mainViewModel.recentConversations.collectAsState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("VoiceCore Assistant") },
                    actions = {
                        IconButton(onClick = { /* Open settings */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Service Status Card
                ServiceStatusCard(
                    serviceStatus = serviceStatus,
                    isListening = isListening,
                    onServiceToggle = { isRunning ->
                        if (isRunning) {
                            voiceServiceViewModel.stopVoiceService()
                        } else {
                            voiceServiceViewModel.startVoiceService()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Permission Requirements
                PermissionSection(
                    audioPermissionState = audioPermissionState,
                    phonePermissionState = phonePermissionState,
                    phoneStatePermissionState = phoneStatePermissionState,
                    notificationPermissionState = notificationPermissionState,
                    overlayPermissionState = overlayPermissionState
                )

                // Voice Input Status
                if (currentTranscript.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Current Input:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentTranscript,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Waveform Visualizer
                if (isListening) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Listening...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            WaveformVisualizer(
                                isActive = isListening,
                                modifier = Modifier.height(100.dp)
                            )
                        }
                    }
                }

                // Recent Conversations
                if (recentConversations.isNotEmpty()) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    recentConversations.take(5).forEach { conversation ->
                        ConversationCard(conversation = conversation)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PermissionSection(
    audioPermissionState: PermissionState,
    phonePermissionState: PermissionState,
    phoneStatePermissionState: PermissionState,
    notificationPermissionState: PermissionState,
    overlayPermissionState: PermissionState
) {
    Text(
        text = "Permissions Required",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    
    PermissionCard(
        permission = Manifest.permission.RECORD_AUDIO,
        permissionState = audioPermissionState,
        icon = Icons.Default.Mic,
        title = "Microphone Access",
        description = "Required for voice input and wake word detection"
    )
    
    PermissionCard(
        permission = Manifest.permission.CALL_PHONE,
        permissionState = phonePermissionState,
        icon = Icons.Default.Call,
        title = "Phone Access",
        description = "Required for automatic call answering"
    )
    
    PermissionCard(
        permission = Manifest.permission.READ_PHONE_STATE,
        permissionState = phoneStatePermissionState,
        icon = Icons.Default.Listen,
        title = "Phone State",
        description = "Required to detect incoming calls"
    )
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionCard(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            permissionState = notificationPermissionState,
            icon = Icons.Default.VolumeUp,
            title = "Notifications",
            description = "Required for service status and alerts"
        )
    }
    
    PermissionCard(
        permission = Manifest.permission.SYSTEM_ALERT_WINDOW,
        permissionState = overlayPermissionState,
        icon = Icons.Default.VolumeUp,
        title = "System Overlays",
        description = "Required for call screen integration"
    )
}

@Composable
fun ConversationCard(conversation: com.assistant.voicecore.model.ConversationLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "User: ${conversation.inputText}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Assistant: ${conversation.gptResponse}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${conversation.createdAt}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}