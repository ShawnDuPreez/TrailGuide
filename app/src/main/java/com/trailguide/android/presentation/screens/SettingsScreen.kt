package com.trailguide.android.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trailguide.android.R
import com.trailguide.android.data.datastore.UserPreferences
import com.trailguide.android.presentation.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Settings screen with Material 3 design.
 * Displays preferences for biometric auth, notifications, language, sync, and app info.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val language by viewModel.languageFlow.collectAsState(initial = UserPreferences.LANGUAGE_ENGLISH)
    val notificationsEnabled by viewModel.notificationsEnabledFlow.collectAsState(initial = true)
    val biometricEnabled by viewModel.biometricEnabledFlow.collectAsState(initial = false)
    val weatherAlerts by viewModel.weatherAlertsFlow.collectAsState(initial = true)
    val friendActivity by viewModel.friendActivityFlow.collectAsState(initial = true)
    val newTrails by viewModel.newTrailsFlow.collectAsState(initial = true)
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // General Section
            SettingsSectionHeader(title = stringResource(R.string.settings_general))
            
            // Language Setting
            SettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_language),
                subtitle = getLanguageName(language),
                onClick = { showLanguageDialog = true }
            )
            
            Divider()
            
            // Security Section
            SettingsSectionHeader(title = stringResource(R.string.settings_security))
            
            // Biometric Authentication
            if (uiState.biometricAvailable) {
                SettingsSwitchItem(
                    icon = Icons.Default.Fingerprint,
                    title = stringResource(R.string.settings_biometric),
                    subtitle = stringResource(R.string.settings_biometric_description),
                    checked = biometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) }
                )
                Divider()
            }
            
            // Notifications Section
            SettingsSectionHeader(title = stringResource(R.string.settings_notifications))
            
            SettingsSwitchItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.settings_notifications),
                subtitle = stringResource(R.string.notifications_enabled),
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )
            
            if (notificationsEnabled) {
                SettingsSwitchItem(
                    icon = Icons.Default.Cloud,
                    title = stringResource(R.string.notifications_weather),
                    subtitle = "Get alerts about weather conditions",
                    checked = weatherAlerts,
                    onCheckedChange = { viewModel.setWeatherAlerts(it) },
                    indent = true
                )
                
                SettingsSwitchItem(
                    icon = Icons.Default.Terrain,
                    title = stringResource(R.string.notifications_new_trails),
                    subtitle = "Get notified about new trails",
                    checked = newTrails,
                    onCheckedChange = { viewModel.setNewTrails(it) },
                    indent = true
                )
                
                SettingsSwitchItem(
                    icon = Icons.Default.People,
                    title = stringResource(R.string.notifications_friends),
                    subtitle = "See when friends review trails",
                    checked = friendActivity,
                    onCheckedChange = { viewModel.setFriendActivity(it) },
                    indent = true
                )
            }
            
            Divider()
            
            // Sync Section
            SettingsSectionHeader(title = stringResource(R.string.settings_sync))
            
            SettingsItem(
                icon = Icons.Default.Sync,
                title = stringResource(R.string.settings_sync_now),
                subtitle = uiState.syncMessage ?: getLastSyncTime(uiState.lastSyncTime),
                onClick = { viewModel.triggerManualSync() },
                trailing = {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            )
            
            Divider()
            
            // About Section
            SettingsSectionHeader(title = stringResource(R.string.settings_about))
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_version),
                subtitle = viewModel.getAppVersion(),
                onClick = { }
            )
        }
        
        // Language Selection Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = language,
                onLanguageSelected = { selectedLanguage ->
                    viewModel.setLanguage(selectedLanguage)
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = trailing ?: {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    indent: Boolean = false
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = if (indent) Modifier.padding(start = 16.dp) else Modifier
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = if (indent) Modifier.padding(start = 16.dp) else Modifier
    )
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column {
                LanguageOption(
                    name = stringResource(R.string.language_english),
                    code = UserPreferences.LANGUAGE_ENGLISH,
                    selected = currentLanguage == UserPreferences.LANGUAGE_ENGLISH,
                    onSelected = onLanguageSelected
                )
                LanguageOption(
                    name = stringResource(R.string.language_afrikaans),
                    code = UserPreferences.LANGUAGE_AFRIKAANS,
                    selected = currentLanguage == UserPreferences.LANGUAGE_AFRIKAANS,
                    onSelected = onLanguageSelected
                )
                LanguageOption(
                    name = stringResource(R.string.language_zulu),
                    code = UserPreferences.LANGUAGE_ZULU,
                    selected = currentLanguage == UserPreferences.LANGUAGE_ZULU,
                    onSelected = onLanguageSelected
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LanguageOption(
    name: String,
    code: String,
    selected: Boolean,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(code) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = { onSelected(code) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(name)
    }
}

@Composable
private fun getLanguageName(code: String): String {
    return when (code) {
        UserPreferences.LANGUAGE_ENGLISH -> stringResource(R.string.language_english)
        UserPreferences.LANGUAGE_AFRIKAANS -> stringResource(R.string.language_afrikaans)
        UserPreferences.LANGUAGE_ZULU -> stringResource(R.string.language_zulu)
        else -> stringResource(R.string.language_english)
    }
}

private fun getLastSyncTime(timestamp: Long?): String {
    if (timestamp == null) return "Never synced"
    
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return "Last synced: ${dateFormat.format(Date(timestamp))}"
}

