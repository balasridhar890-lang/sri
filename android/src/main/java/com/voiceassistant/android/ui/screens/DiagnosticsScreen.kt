package com.voiceassistant.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceassistant.android.ui.theme.AppTheme
import com.voiceassistant.android.viewmodel.DiagnosticsViewModel
import com.voiceassistant.android.viewmodel.DiagnosticsState
import com.voiceassistant.android.viewmodel.LogEntry
import com.voiceassistant.android.viewmodel.SystemInfo

/**
 * Diagnostics and logging screen
 */
@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DiagnosticsContent(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onRefreshLogs = viewModel::refreshLogs,
                onClearLogs = viewModel::clearLogs,
                onExportLogs = viewModel::exportLogs,
                onCopyLogs = viewModel::copyLogsToClipboard,
                onTestBackend = viewModel::testBackendConnection,
                onTestPermissions = viewModel::testPermissions
            )
        }
    }
}

@Composable
private fun DiagnosticsContent(
    uiState: DiagnosticsState,
    onNavigateBack: () -> Unit,
    onRefreshLogs: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
    onCopyLogs: () -> Unit,
    onTestBackend: () -> Unit,
    onTestPermissions: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Diagnostics") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                // Test dropdown
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Tests"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Test Backend") },
                            onClick = {
                                onTestBackend()
                                expanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null) }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Test Permissions") },
                            onClick = {
                                onTestPermissions()
                                expanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) }
                        )
                    }
                }
            }
        )
        
        // Test Results
        uiState.testResults.forEach { result ->
            TestResultCard(result = result)
        }
        
        // System Information
        SystemInfoCard(systemInfo = uiState.systemInfo)
        
        // Log Controls
        LogControlsSection(
            onRefresh = onRefreshLogs,
            onClear = onClearLogs,
            onExport = onExportLogs,
            onCopy = onCopyLogs,
            logCount = uiState.logs.size
        )
        
        // Log Level Filter
        LogLevelFilterSection(
            selectedLevel = uiState.selectedLogLevel,
            onLevelSelected = viewModel::setLogLevelFilter,
            availableLevels = uiState.availableLogLevels
        )
        
        // Logs List
        LogsList(
            logs = uiState.logs,
            isLoading = uiState.isLoadingLogs
        )
        
        // Error Message
        uiState.error?.let { error ->
            ErrorCard(
                error = error,
                onDismiss = viewModel::clearError
            )
        }
    }
}

@Composable
private fun TestResultCard(
    result: com.voiceassistant.android.viewmodel.TestResult
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (result.success) {
                true -> MaterialTheme.colorScheme.primaryContainer
                false -> MaterialTheme.colorScheme.errorContainer
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    imageVector = when {
                        result.success == true -> Icons.Default.CheckCircle
                        result.success == false -> Icons.Default.Error
                        else -> Icons.Default.HourglassEmpty
                    },
                    contentDescription = null,
                    tint = when (result.success) {
                        true -> Color(0xFF4CAF50)
                        false -> MaterialTheme.colorScheme.error
                        null -> MaterialTheme.colorScheme.primary
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = result.testName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = result.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (result.message.isNotEmpty()) {
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (result.success) {
                        true -> MaterialTheme.colorScheme.onPrimaryContainer
                        false -> MaterialTheme.colorScheme.onErrorContainer
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun SystemInfoCard(
    systemInfo: SystemInfo
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "System Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            
            // Android Version
            SystemInfoRow(
                label = "Android Version",
                value = "${systemInfo.androidVersion} (API ${systemInfo.sdkVersion})"
            )
            
            // Device Model
            SystemInfoRow(
                label = "Device Model",
                value = systemInfo.deviceModel
            )
            
            // App Version
            SystemInfoRow(
                label = "App Version",
                value = "${systemInfo.appVersion} (${systemInfo.appVersionCode})"
            )
            
            // Available Processors
            SystemInfoRow(
                label = "Available Processors",
                value = systemInfo.availableProcessors.toString()
            )
            
            // Memory Info
            SystemInfoRow(
                label = "Total Memory",
                value = "%.1f GB".format(systemInfo.totalMemory / (1024.0 * 1024.0 * 1024.0))
            )
            
            // Free Memory
            SystemInfoRow(
                label = "Available Memory",
                value = "%.1f GB".format(systemInfo.availableMemory / (1024.0 * 1024.0 * 1024.0))
            )
            
            // Storage
            SystemInfoRow(
                label = "Internal Storage",
                value = "${systemInfo.totalStorage}GB total, ${systemInfo.availableStorage}GB available"
            )
            
            // Network Status
            SystemInfoRow(
                label = "Network",
                value = "${systemInfo.networkType} (${systemInfo.networkState})"
            )
            
            // Permissions Status
            SystemInfoRow(
                label = "Permissions",
                value = "${systemInfo.grantedPermissions}/${systemInfo.requestedPermissions} granted"
            )
        }
    }
}

@Composable
private fun SystemInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LogControlsSection(
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit,
    onCopy: () -> Unit,
    logCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                    text = "Logs ($logCount entries)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh")
                }
                
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
                
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }
            }
        }
    }
}

@Composable
private fun LogLevelFilterSection(
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    availableLevels: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Log Level Filter",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableLevels.forEach { level ->
                    FilterChip(
                        selected = selectedLevel == level,
                        onClick = { onLevelSelected(level) },
                        label = { Text(level) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LogsList(
    logs: List<LogEntry>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else if (logs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "No Logs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "No log entries match the current filter",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { logEntry ->
                LogEntryCard(logEntry = logEntry)
            }
        }
    }
}

@Composable
private fun LogEntryCard(
    logEntry: LogEntry
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Log Level Indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(getLogLevelColor(logEntry.level)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Empty box for color indicator
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = logEntry.level,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = getLogLevelColor(logEntry.level)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = logEntry.tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = logEntry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            SelectionContainer {
                Text(
                    text = logEntry.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun getLogLevelColor(level: String): Color {
    return when (level.uppercase()) {
        "VERBOSE" -> Color(0xFF757575)
        "DEBUG" -> Color(0xFF2196F3)
        "INFO" -> Color(0xFF4CAF50)
        "WARN" -> Color(0xFFFF9800)
        "ERROR" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
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
                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}