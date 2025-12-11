package com.assistant.voicecore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.assistant.voicecore.model.VoiceServiceState
import com.assistant.voicecore.model.VoiceServiceStatus

/**
 * Card component for displaying and controlling voice service status
 */
@Composable
fun ServiceStatusCard(
    serviceStatus: VoiceServiceStatus,
    isListening: Boolean,
    onServiceToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with service status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceStatusIcon(
                    state = serviceStatus.currentState,
                    isActive = serviceStatus.isRunning
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Voice Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getStatusText(serviceStatus.currentState),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Service toggle button
                ServiceToggleButton(
                    isRunning = serviceStatus.isRunning,
                    isListening = isListening,
                    onToggle = onServiceToggle
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status indicators
            if (serviceStatus.isRunning) {
                StatusIndicators(
                    serviceStatus = serviceStatus,
                    isListening = isListening
                )
            }
            
            // Error message
            serviceStatus.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                ErrorMessage(error = error)
            }
        }
    }
}

@Composable
fun ServiceStatusIcon(
    state: VoiceServiceState,
    isActive: Boolean
) {
    val (icon, color) = when (state) {
        VoiceServiceState.IDLE -> {
            if (isActive) Icons.Default.Pause to MaterialTheme.colorScheme.secondary
            else Icons.Default.Stop to MaterialTheme.colorScheme.error
        }
        VoiceServiceState.INITIALIZING -> {
            Icons.Default.Refresh to MaterialTheme.colorScheme.primary
        }
        VoiceServiceState.LISTENING -> {
            Icons.Default.Mic to MaterialTheme.colorScheme.primary
        }
        VoiceServiceState.PROCESSING -> {
            Icons.Default.Psychology to MaterialTheme.colorScheme.tertiary
        }
        VoiceServiceState.SPEAKING -> {
            Icons.Default.VolumeUp to MaterialTheme.colorScheme.primary
        }
        VoiceServiceState.CALL_ANSWERING -> {
            Icons.Default.Call to MaterialTheme.colorScheme.primary
        }
        VoiceServiceState.ERROR -> {
            Icons.Default.Error to MaterialTheme.colorScheme.error
        }
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "Service Status",
        tint = color,
        modifier = Modifier.size(32.dp)
    )
}

@Composable
fun ServiceToggleButton(
    isRunning: Boolean,
    isListening: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val (buttonText, buttonColor) = if (isRunning) {
        "Stop" to MaterialTheme.colorScheme.error
    } else {
        "Start" to MaterialTheme.colorScheme.primary
    }
    
    Button(
        onClick = { onToggle(!isRunning) },
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        )
    ) {
        Text(
            text = buttonText,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun StatusIndicators(
    serviceStatus: VoiceServiceStatus,
    isListening: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Listening indicator
        StatusIndicator(
            icon = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
            text = if (isListening) "Listening" else "Silent",
            isActive = isListening
        )
        
        // Battery optimization indicator
        StatusIndicator(
            icon = if (serviceStatus.batteryOptimizationExempt) 
                Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
            text = "Battery Opt",
            isActive = serviceStatus.batteryOptimizationExempt
        )
        
        // Foreground service indicator
        StatusIndicator(
            icon = if (serviceStatus.foregroundServiceActive) 
                Icons.Default.Service else Icons.Default.Service,
            text = "Service",
            isActive = serviceStatus.foregroundServiceActive
        )
    }
}

@Composable
fun StatusIndicator(
    icon: ImageVector,
    text: String,
    isActive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun ErrorMessage(error: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

private fun getStatusText(state: VoiceServiceState): String {
    return when (state) {
        VoiceServiceState.IDLE -> "Ready to start"
        VoiceServiceState.INITIALIZING -> "Initializing services..."
        VoiceServiceState.LISTENING -> "Listening for \"Hey Jarvis\""
        VoiceServiceState.PROCESSING -> "Processing your request..."
        VoiceServiceState.SPEAKING -> "Speaking response..."
        VoiceServiceState.CALL_ANSWERING -> "Managing call..."
        VoiceServiceState.ERROR -> "Error occurred"
    }
}