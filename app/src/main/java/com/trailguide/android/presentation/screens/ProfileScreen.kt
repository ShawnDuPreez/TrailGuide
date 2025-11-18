package com.trailguide.android.presentation.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.trailguide.android.BuildConfig
import com.trailguide.android.R
import com.trailguide.android.data.model.Language
import com.trailguide.android.data.notification.TrailNotificationManager
import com.trailguide.android.presentation.theme.*
import com.trailguide.android.presentation.viewmodel.ProfileViewModel
import androidx.activity.ComponentActivity

/**
 * Profile and settings screen.
 * Handles user authentication with Supabase, language selection, and app preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val currentUser by viewModel.currentUser.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    val notificationTime by viewModel.notificationTime.collectAsState()
    val showTimePickerDialog by viewModel.showTimePickerDialog.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, show time picker
            viewModel.setNotificationsEnabled(true)
        } else {
            // Permission denied, keep notifications disabled
            viewModel.hideTimePicker()
        }
    }
    
    // Check if notification permission is needed (Android 13+)
    val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !TrailNotificationManager.canPostNotifications(context)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Error/Success messages
        errorMessage?.let { error ->
            Card(colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.weight(1f),
                        color = Error
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(Icons.Default.Close, stringResource(R.string.dismiss), tint = Error)
                    }
                }
            }
        }
        
        successMessage?.let { success ->
            Card(colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f))) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = success,
                        modifier = Modifier.weight(1f),
                        color = Success
                    )
                    IconButton(onClick = { viewModel.clearSuccessMessage() }) {
                        Icon(Icons.Default.Close, stringResource(R.string.dismiss), tint = Success)
                    }
                }
            }
        }
        
        // User authentication card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Text(stringResource(R.string.authentication_supabase), style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // User avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.email?.firstOrNull()?.uppercase() ?: "G"),
                            style = MaterialTheme.typography.titleLarge,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSignedIn) currentUser?.email ?: stringResource(R.string.user) else stringResource(R.string.guest),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.supabase_auth),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    
                    if (!isSignedIn) {
                        Button(
                            onClick = { viewModel.signInWithGoogle() }
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.sign_in))
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.signOut() }) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.sign_out))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Info text about Supabase OAuth
                Text(
                    stringResource(R.string.sign_in_with_google_supabase),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        
        // Email/Password sign-in card (optional)
        if (!isSignedIn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.or_sign_in_with_email),
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var displayName by remember { mutableStateOf("") }
                    var isRegistering by remember { mutableStateOf(false) }
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                        )
                    )
                    
                    if (isRegistering) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text(stringResource(R.string.display_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isRegistering) {
                                    viewModel.registerWithEmail(email, password, displayName)
                                } else {
                                    viewModel.signInWithEmail(email, password)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = email.isNotBlank() && password.isNotBlank() && 
                                     (!isRegistering || displayName.isNotBlank())
                        ) {
                            Text(if (isRegistering) stringResource(R.string.register) else stringResource(R.string.sign_in))
                        }
                        
                        OutlinedButton(
                            onClick = {
                                isRegistering = !isRegistering
                                displayName = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isRegistering) stringResource(R.string.back_to_sign_in) else stringResource(R.string.register))
                        }
                    }
                    
                    if (!isRegistering) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.note_google_sign_in),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Biometric authentication card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Text(stringResource(R.string.biometric_login), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.enable_biometric_authentication), color = TextSecondary)
                    Switch(
                        checked = userPreferences.biometricsEnabled,
                        onCheckedChange = { viewModel.setBiometricsEnabled(it) }
                    )
                }
            }
        }
        
        // Notifications card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Text(stringResource(R.string.notifications), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.trail_reminders_safety_alerts), color = TextSecondary)
                    Switch(
                        checked = userPreferences.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // Check if notification permission is needed (Android 13+)
                                if (needsNotificationPermission) {
                                    // Request notification permission first
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    // Permission already granted or not needed, show time picker
                                    viewModel.setNotificationsEnabled(true)
                                }
                            } else {
                                // Disable notifications
                                viewModel.setNotificationsEnabled(false)
                            }
                        }
                    )
                }
                
                // Show notification time and edit button if enabled
                if (userPreferences.notificationsEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.notifications_at, String.format("%02d:%02d", notificationTime.first, notificationTime.second)),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Test notification button (for testing)
                            IconButton(onClick = { viewModel.testNotificationNow() }) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = stringResource(R.string.test_notification))
                            }
                            IconButton(onClick = { viewModel.showTimePicker() }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_notification_time))
                            }
                        }
                    }
                }
                
                // Test notification button (always visible for testing)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.testNotificationNow() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.test_notification_now))
                }
            }
        }
        
        // Time Picker Dialog
        if (showTimePickerDialog) {
            val timePickerState = rememberTimePickerState(
                initialHour = notificationTime.first,
                initialMinute = notificationTime.second,
                is24Hour = true
            )
            
            AlertDialog(
                onDismissRequest = { viewModel.hideTimePicker() },
                title = { Text(stringResource(R.string.select_notification_time)) },
                text = {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialSelectedContentColor = MaterialTheme.colorScheme.primary,
                            clockDialColor = MaterialTheme.colorScheme.primaryContainer,
                            selectorColor = MaterialTheme.colorScheme.primary,
                            periodSelectorBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val hour = timePickerState.hour
                            val minute = timePickerState.minute
                            
                            // Check if notifications are already enabled
                            if (userPreferences.notificationsEnabled) {
                                // Just update the time
                                viewModel.setNotificationTime(hour, minute)
                                viewModel.hideTimePicker()
                            } else {
                                // Enable notifications with selected time
                                if (needsNotificationPermission) {
                                    // Request permission first, then enable
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    // Store time temporarily - will be applied after permission granted
                                    viewModel.setNotificationTime(hour, minute)
                                    viewModel.hideTimePicker()
                                } else {
                                    // Enable immediately
                                    viewModel.enableNotificationsWithTime(hour, minute)
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideTimePicker() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        
        // Language selection card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null)
                    Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Language.entries.forEach { language ->
                        FilterChip(
                            selected = userPreferences.language == language,
                            onClick = { 
                                viewModel.setLanguage(language, onLanguageChanged = {
                                    // Recreate activity to apply new locale
                                    activity?.recreate()
                                })
                            },
                            label = { Text(language.code.uppercase()) }
                        )
                    }
                }
            }
        }
        
        // App info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(stringResource(R.string.app_info), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), color = TextSecondary)
                Text(stringResource(R.string.native_kotlin_mvvm), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.supabase_auth_database), color = Primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
