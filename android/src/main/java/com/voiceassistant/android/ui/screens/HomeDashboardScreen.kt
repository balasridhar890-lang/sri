package com.voiceassistant.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceassistant.android.ui.theme.AppTheme
import com.voiceassistant.android.viewmodel.HomeDashboardViewModel
import com.voiceassistant.android.viewmodel.HomeDashboardState
import com.voiceassistant.android.viewmodel.VoiceStatus
import com.voiceassistant.android.viewmodel.BatteryInfo
import com.voiceassistant.android.viewmodel.AppStats

/**
 * Home dashboard screen with voice status, battery info, and recent activity
 */
@Composable
fun HomeDashboardScreen(
    onNavigateToContacts: () -> Unit,
    onNavigateToConversationHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HomeDashboardContent(
                uiState = uiState,
                onRefresh = { viewModel.refreshDashboard() },
                onForceSync = { viewModel.forceSync() },
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToConversationHistory = onNavigateToConversationHistory,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun HomeDashboardContent(
    uiState: HomeDashboardState,
    onRefresh: () -> Unit,
    onForceSync: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToConversationHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Header(
            isLoading = uiState.isLoading,
            onRefresh = onRefresh
        )
        
        // Voice Status Card
        VoiceStatusCard(
            voiceStatus = uiState.voiceStatus
        )
        
        // Quick Actions Grid
        QuickActionsGrid(
            onNavigateToContacts = onNavigateToContacts,
            onNavigateToConversationHistory = onNavigateToConversationHistory,
            onNavigateToSettings = onNavigateToSettings
        )
        
        // System Status Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BatteryStatusCard(
                batteryInfo = uiState.batteryInfo,
                modifier = Modifier.weight(1f)
            )
            AppStatsCard(
                stats = uiState.stats,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Recent Activity
        RecentActivitySection(
            recentCalls = uiState.recentCalls,
            recentSMS = uiState.recentSMS,
            onForceSync = onForceSync
        )
        
        // Running Apps (if available)
        if (uiState.runningApps.isNotEmpty()) {
            RunningAppsSection(
                apps = uiState.runningApps
            )
        }
        
        // Error Message
        uiState.error?.let { error ->
            ErrorMessageCard(
                error = error,
                onDismiss = onRefresh
            )
        }
    }
}

@Composable
private fun Header(
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Voice Assistant",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Your intelligent assistant",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(
            onClick = onRefresh,
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }
    }
}

@Composable
private fun VoiceStatusCard(
    voiceStatus: VoiceStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VoiceStatusIcon(
                status = voiceStatus,
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = getStatusText(voiceStatus),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = getStatusDescription(voiceStatus),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VoiceStatusIcon(
    status: VoiceStatus,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (status) {
        VoiceStatus.IDLE -> Icons.Default.Mic to MaterialTheme.colorScheme.primary
        VoiceStatus.LISTENING -> Icons.Default.Mic to Color(0xFF4CAF50)
        VoiceStatus.PROCESSING -> Icons.Default.Psychology to Color(0xFFFF9800)
        VoiceStatus.SPEAKING -> Icons.Default.VolumeUp to Color(0xFF2196F3)
        VoiceStatus.ERROR -> Icons.Default.Error to Color(0xFFF44336)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Voice Status",
            modifier = Modifier.size(32.dp),
            tint = color
        )
    }
}

@Composable
private fun getStatusText(status: VoiceStatus): String {
    return when (status) {
        VoiceStatus.IDLE -> "Ready"
        VoiceStatus.LISTENING -> "Listening..."
        VoiceStatus.PROCESSING -> "Processing"
        VoiceStatus.SPEAKING -> "Speaking"
        VoiceStatus.ERROR -> "Error"
    }
}

@Composable
private fun getStatusDescription(status: VoiceStatus): String {
    return when (status) {
        VoiceStatus.IDLE -> "Assistant is ready and waiting"
        VoiceStatus.LISTENING -> "Assistant is listening for your voice"
        VoiceStatus.PROCESSING -> "Assistant is processing your request"
        VoiceStatus.SPEAKING -> "Assistant is responding"
        VoiceStatus.ERROR -> "There's an issue with the voice service"
    }
}

@Composable
private fun QuickActionsGrid(
    onNavigateToContacts: () -> Unit,
    onNavigateToConversationHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val actions = listOf(
        QuickAction(
            title = "Contacts",
            icon = Icons.Default.Contacts,
            onClick = onNavigateToContacts
        ),
        QuickAction(
            title = "History",
            icon = Icons.Default.History,
            onClick = onNavigateToConversationHistory
        ),
        QuickAction(
            title = "Settings",
            icon = Icons.Default.Settings,
            onClick = onNavigateToSettings
        ),
        QuickAction(
            title = "Sync",
            icon = Icons.Default.CloudSync,
            onClick = { /* Sync handled by refresh */ }
        )
    )
    
    LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
        modifier = Modifier.heightIn(max = 240.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { action ->
            QuickActionCard(action = action)
        }
    }
}

@Composable
private fun QuickActionCard(action: QuickAction) {
    Card(
        onClick = action.onClick,
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun BatteryStatusCard(
    batteryInfo: BatteryInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                    contentDescription = "Battery",
                    tint = getBatteryColor(batteryInfo.level)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "${batteryInfo.level}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = batteryInfo.status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (batteryInfo.temperature > 0) {
                Text(
                    text = "${batteryInfo.temperature}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getBatteryColor(level: Int): Color {
    return when {
        level > 80 -> Color(0xFF4CAF50)
        level > 50 -> Color(0xFFFF9800)
        level > 20 -> Color(0xFFFF5722)
        else -> Color(0xFFF44336)
    }
}

@Composable
private fun AppStatsCard(
    stats: AppStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            StatRow(
                label = "Total Calls",
                value = stats.totalCalls.toString()
            )
            
            StatRow(
                label = "Total SMS",
                value = stats.totalSmsLogged.toString()
            )
            
            StatRow(
                label = "Auto-Reply",
                value = "${(stats.autoReplyRate * 100).toInt()}%"
            )
            
            if (stats.avgCallDuration > 0) {
                StatRow(
                    label = "Avg Duration",
                    value = "${stats.avgCallDuration / 60}m"
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RecentActivitySection(
    recentCalls: List<com.voiceassistant.android.viewmodel.CallLogItem>,
    recentSMS: List<com.voiceassistant.android.viewmodel.SMSLogItem>,
    onForceSync: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                
                TextButton(
                    onClick = onForceSync
                ) {
                    Text("Sync All")
                }
            }
            
            if (recentCalls.isNotEmpty() || recentSMS.isNotEmpty()) {
                // Recent Calls
                if (recentCalls.isNotEmpty()) {
                    Text(
                        text = "Recent Calls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    recentCalls.take(3).forEach { call ->
                        ActivityRow(
                            icon = if (call.direction == "incoming") Icons.Default.PhoneInbound else Icons.Default.PhoneOutbound,
                            title = call.phoneNumber,
                            subtitle = "${call.formattedTime} • ${call.formattedDuration}",
                            success = call.success
                        )
                    }
                }
                
                // Recent SMS
                if (recentSMS.isNotEmpty()) {
                    Text(
                        text = "Recent SMS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    recentSMS.take(3).forEach { sms ->
                        ActivityRow(
                            icon = Icons.Default.Message,
                            title = sms.phoneNumber,
                            subtitle = sms.formattedTime,
                            success = sms.decision == "yes"
                        )
                    }
                }
            } else {
                Text(
                    text = "No recent activity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ActivityRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    success: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (success) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun RunningAppsSection(
    apps: List<com.voiceassistant.android.viewmodel.RunningApp>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Running Apps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            
            apps.take(5).forEach { app ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorMessageCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            TextButton(
                onClick = onDismiss
            ) {
                Text("Dismiss")
            }
        }
    }
}