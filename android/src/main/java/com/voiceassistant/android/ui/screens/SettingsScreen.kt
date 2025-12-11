package com.voiceassistant.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceassistant.android.ui.theme.AppTheme
import com.voiceassistant.android.viewmodel.SettingsViewModel
import com.voiceassistant.android.viewmodel.SettingsState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Settings screen with all configuration options
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SettingsContent(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onNavigateToDiagnostics = onNavigateToDiagnostics,
                onUpdateSetting = { key, value -> 
                    scope.launch { viewModel.updateSetting(key, value) }
                },
                onUpdateApiConfig = { url, userId ->
                    scope.launch { viewModel.updateApiConfig(url, userId) }
                },
                onUpdateApiCredentials = { apiKey, apiToken ->
                    scope.launch { viewModel.updateApiCredentials(apiKey, apiToken) }
                },
                onUpdateVoiceSettings = { sensitivity, volume, language, gender ->
                    scope.launch { viewModel.updateVoiceSettings(sensitivity, volume, language, gender) }
                },
                onUpdateFeatureToggles = { autoAnswer, autoReply, voiceActivation, callMonitoring, smsMonitoring, contactSync ->
                    scope.launch { viewModel.updateFeatureToggles(autoAnswer, autoReply, voiceActivation, callMonitoring, smsMonitoring, contactSync) }
                },
                onUpdateUiPreferences = { darkMode, accessibility ->
                    scope.launch { viewModel.updateUiPreferences(darkMode, accessibility) }
                },
                onUpdateDebugSettings = { debugMode, logLevel, performanceMonitoring ->
                    scope.launch { viewModel.updateDebugSettings(debugMode, logLevel, performanceMonitoring) }
                },
                onSyncWithBackend = viewModel::syncWithBackend,
                onForceSyncFromBackend = viewModel::forceSyncFromBackend,
                onResetToDefaults = viewModel::resetToDefaults,
                onExportSettings = { 
                    scope.launch { 
                        val settings = viewModel.exportSettings()
                        // Handle export (e.g., share or save)
                    }
                },
                onClearErrors = viewModel::clearErrors
            )
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsState,
    onNavigateBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onUpdateSetting: (String, Any) -> Unit,
    onUpdateApiConfig: (String, Int) -> Unit,
    onUpdateApiCredentials: (String?, String?) -> Unit,
    onUpdateVoiceSettings: (Float, Float, String, String) -> Unit,
    onUpdateFeatureToggles: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onUpdateUiPreferences: (Boolean, Boolean) -> Unit,
    onUpdateDebugSettings: (Boolean, String, Boolean) -> Unit,
    onSyncWithBackend: () -> Unit,
    onForceSyncFromBackend: () -> Unit,
    onResetToDefaults: () -> Unit,
    onExportSettings: () -> Unit,
    onClearErrors: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                // Sync status
                if (uiState.hasUnsavedChanges) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Text("${uiState.pendingChangesCount}")
                    }
                }
                
                // More options
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Settings") },
                            onClick = {
                                onExportSettings()
                                expanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Diagnostics") },
                            onClick = {
                                onNavigateToDiagnostics()
                                expanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Reset to Defaults") },
                            onClick = {
                                onResetToDefaults()
                                expanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) }
                        )
                    }
                }
            }
        )
        
        // Error Messages
        uiState.syncError?.let { error ->
            ErrorCard(
                title = "Sync Error",
                message = error,
                onDismiss = onClearErrors
            )
        }
        
        uiState.saveError?.let { error ->
            ErrorCard(
                title = "Save Error",
                message = error,
                onDismiss = onClearErrors
            )
        }
        
        // Loading State
        if (uiState.isLoading && uiState.preferences == null) {
            LoadingContent()
            return@Column
        }
        
        uiState.preferences?.let { preferences ->
            // Sync Status Card
            SyncStatusCard(
                isSyncing = uiState.isSyncing,
                hasUnsavedChanges = uiState.hasUnsavedChanges,
                lastSyncTime = uiState.lastSyncTime,
                pendingChangesCount = uiState.pendingChangesCount,
                onSyncWithBackend = onSyncWithBackend,
                onForceSyncFromBackend = onForceSyncFromBackend
            )
            
            // API Configuration Section
            ApiConfigurationSection(
                preferences = preferences,
                onUpdateApiConfig = onUpdateApiConfig,
                onUpdateApiCredentials = onUpdateApiCredentials
            )
            
            // Feature Toggles Section
            FeatureTogglesSection(
                preferences = preferences,
                onUpdateFeatureToggles = onUpdateFeatureToggles
            )
            
            // Voice Settings Section
            VoiceSettingsSection(
                preferences = preferences,
                onUpdateVoiceSettings = onUpdateVoiceSettings
            )
            
            // UI Preferences Section
            UiPreferencesSection(
                preferences = preferences,
                onUpdateUiPreferences = onUpdateUiPreferences
            )
            
            // Debug Settings Section
            DebugSettingsSection(
                preferences = preferences,
                onUpdateDebugSettings = onUpdateDebugSettings
            )
        }
    }
}

@Composable
private fun SyncStatusCard(
    isSyncing: Boolean,
    hasUnsavedChanges: Boolean,
    lastSyncTime: Long,
    pendingChangesCount: Int,
    onSyncWithBackend: () -> Unit,
    onForceSyncFromBackend: () -> Unit
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
                    text = "Synchronization",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Status:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = when {
                        isSyncing -> "Syncing..."
                        hasUnsavedChanges -> "Pending changes"
                        else -> "Up to date"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isSyncing -> MaterialTheme.colorScheme.primary
                        hasUnsavedChanges -> MaterialTheme.colorScheme.secondary
                        else -> Color(0xFF4CAF50)
                    }
                )
            }
            
            if (hasUnsavedChanges) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pending changes:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = pendingChangesCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            if (lastSyncTime > 0) {
                Text(
                    text = "Last sync: ${java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(lastSyncTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSyncWithBackend,
                    enabled = !isSyncing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync")
                }
                
                OutlinedButton(
                    onClick = onForceSyncFromBackend,
                    enabled = !isSyncing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pull")
                }
            }
        }
    }
}

@Composable
private fun ApiConfigurationSection(
    preferences: com.voiceassistant.android.repository.UserPreferences,
    onUpdateApiConfig: (String, Int) -> Unit,
    onUpdateApiCredentials: (String?, String?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "API Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            
            // Backend URL
            OutlinedTextField(
                value = preferences.backendUrl ?: "",
                onValueChange = { onUpdateApiConfig(it, preferences.userId) },
                label = { Text("Backend URL") },
                placeholder = { Text("http://localhost:8000") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
            
            // User ID
            OutlinedTextField(
                value = preferences.userId.toString(),
                onValueChange = { 
                    val userId = it.toIntOrNull() ?: -1
                    onUpdateApiConfig(preferences.backendUrl ?: "", userId)
                },
                label = { Text("User ID") },
                placeholder = { Text("Enter your user ID") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // API Key (password field)
            var showApiKey by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = "", // Don't show current API key for security
                onValueChange = { onUpdateApiCredentials(it, null) },
                label = { Text("API Key") },
                placeholder = { Text("Enter API key (leave empty to keep current)") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Hide" else "Show"
                        )
                    }
                },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // API Token (password field)
            var showApiToken by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = "", // Don't show current API token for security
                onValueChange = { onUpdateApiCredentials(null, it) },
                label = { Text("API Token") },
                placeholder = { Text("Enter API token (leave empty to keep current)") },
                leadingIcon = { Icon(Icons.Default.Token, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showApiToken = !showApiToken }) {
                        Icon(
                            imageVector = if (showApiToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiToken) "Hide" else "Show"
                        )
                    }
                },
                visualTransformation = if (showApiToken) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FeatureTogglesSection(
    preferences: com.voiceassistant.android.repository.UserPreferences,
    onUpdateFeatureToggles: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Feature Toggles",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            
            FeatureToggle(
                title = "Auto Answer",
                description = "Automatically answer incoming calls",
                checked = preferences.autoAnswerEnabled,
                onCheckedChange = { 
                    onUpdateFeatureToggles(
                        it, preferences.autoReplyEnabled, preferences.voiceActivationEnabled,
                        preferences.callMonitoringEnabled, preferences.smsMonitoringEnabled, 
                        preferences.contactSyncEnabled
                    )
                }
            )
            
            FeatureToggle(
                title = "Auto Reply",
                description = "Automatically reply to incoming SMS",
                checked = preferences.autoReplyEnabled,
                onCheckedChange = { 
                    onUpdateFeatureToggles(
                        preferences.autoAnswerEnabled, it, preferences.voiceActivationEnabled,
                        preferences.callMonitoringEnabled, preferences.smsMonitoringEnabled, 
                        preferences.contactSyncEnabled
                    )
                }
            )
            
            FeatureToggle(
                title = "Voice Activation",
                description = "Enable wake word detection",
                checked = preferences.voiceActivationEnabled,
                onCheckedChange = { 
                    onUpdateFeatureToggles(
                        preferences.autoAnswerEnabled, preferences.autoReplyEnabled, it,
                        preferences.callMonitoringEnabled, preferences.smsMonitoringEnabled, 
                        preferences.contactSyncEnabled
                    )
                }
            )
            
            FeatureToggle(
                title = "Call Monitoring",
                description = "Monitor and log call activity",
                checked = preferences.callMonitoringEnabled,
                onCheckedChange = { 
                    onUpdateFeatureToggles(
                        preferences.autoAnswerEnabled, preferences.autoReplyEnabled, 
                        preferences.voiceActivationEnabled, it, preferences.smsMonitoringEnabled, 
                        preferences.contactSyncEnabled
                    )
                }
            )
            
            FeatureToggle(
                title = "SMS Monitoring",
                description = "Monitor and log SMS activity",
                checked = preferences.smsMonitoringEnabled,
                onCheckedChange = { 
                    onUpdateFeatureToggles(
                        preferences.autoAnswerEnabled, preferences.autoReplyEnabled, 
                        preferences.voiceActivationEnabled, preferences.callMonitoringEnabled, it,
                        preferences.contactSyncEnabled
                    )
                }
            )
            
            FeatureToggle(
                title = "Contact Sync",
                description = "Synchronize contacts with app",
                checked = preferences.contactSyncEnabled,
                onCheckedChange = { 
                    onUpdateFeatureToggles(
                        preferences.autoAnswerEnabled, preferences.autoReplyEnabled, 
                        preferences.voiceActivationEnabled, preferences.callMonitoringEnabled, 
                        preferences.smsMonitoringEnabled, it
                    )
                }
            )
        }
    }
}

@Composable
private fun FeatureToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun VoiceSettingsSection(
    preferences: com.voiceassistant.android.repository.UserPreferences,
    onUpdateVoiceSettings: (Float, Float, String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Voice Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            
            // Wake Word Sensitivity
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Wake Word Sensitivity",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Slider(
                    value = preferences.wakeWordSensitivity,
                    onValueChange = { 
                        onUpdateVoiceSettings(
                            it, preferences.voiceResponseVolume, preferences.voiceLanguage, preferences.voiceGender
                        )
                    },
                    valueRange = 0.1f..1.0f,
                    steps = 8
                )
                
                Text(
                    text = "${(preferences.wakeWordSensitivity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
            
            // Voice Response Volume
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Voice Response Volume",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Slider(
                    value = preferences.voiceResponseVolume,
                    onValueChange = { 
                        onUpdateVoiceSettings(
                            preferences.wakeWordSensitivity, it, preferences.voiceLanguage, preferences.voiceGender
                        )
                    },
                    valueRange = 0.0f..1.0f,
                    steps = 9
                )
                
                Text(
                    text = "${(preferences.voiceResponseVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
            
            // Voice Language
            OutlinedTextField(
                value = preferences.voiceLanguage,
                onValueChange = { 
                    onUpdateVoiceSettings(
                        preferences.wakeWordSensitivity, preferences.voiceResponseVolume, it, preferences.voiceGender
                    )
                },
                label = { Text("Voice Language") },
                placeholder = { Text("en-US") },
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Voice Gender
            var expanded by remember { mutableStateOf(false) }
            var selectedGender by remember { mutableStateOf(preferences.voiceGender) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedGender,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Voice Gender") },
                    leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("male", "female", "neutral").forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(gender.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                selectedGender = gender
                                expanded = false
                                onUpdateVoiceSettings(
                                    preferences.wakeWordSensitivity, preferences.voiceResponseVolume, 
                                    preferences.voiceLanguage, gender
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UiPreferencesSection(
    preferences: com.voiceassistant.android.repository.UserPreferences,
    onUpdateUiPreferences: (Boolean, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "UI Preferences",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            
            FeatureToggle(
                title = "Dark Mode",
                description = "Use dark theme",
                checked = preferences.darkModeEnabled,
                onCheckedChange = { onUpdateUiPreferences(it, preferences.accessibilityEnabled) }
            )
            
            FeatureToggle(
                title = "Accessibility",
                description = "Enhanced accessibility features",
                checked = preferences.accessibilityEnabled,
                onCheckedChange = { onUpdateUiPreferences(preferences.darkModeEnabled, it) }
            )
        }
    }
}

@Composable
private fun DebugSettingsSection(
    preferences: com.voiceassistant.android.repository.UserPreferences,
    onUpdateDebugSettings: (Boolean, String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Debug Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            
            FeatureToggle(
                title = "Debug Mode",
                description = "Enable debug logging and features",
                checked = preferences.debugModeEnabled,
                onCheckedChange = { onUpdateDebugSettings(it, preferences.logLevel, preferences.performanceMonitoringEnabled) }
            )
            
            // Log Level
            var expanded by remember { mutableStateOf(false) }
            var selectedLogLevel by remember { mutableStateOf(preferences.logLevel) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedLogLevel,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Log Level") },
                    leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("DEBUG", "INFO", "WARN", "ERROR").forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                selectedLogLevel = level
                                expanded = false
                                onUpdateDebugSettings(
                                    preferences.debugModeEnabled, level, preferences.performanceMonitoringEnabled
                                )
                            }
                        )
                    }
                }
            }
            
            FeatureToggle(
                title = "Performance Monitoring",
                description = "Monitor app performance",
                checked = preferences.performanceMonitoringEnabled,
                onCheckedChange = { onUpdateDebugSettings(preferences.debugModeEnabled, preferences.logLevel, it) }
            )
        }
    }
}

@Composable
private fun ErrorCard(
    title: String,
    message: String,
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
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Text(
                    text = message,
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

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                text = "Loading settings...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}