package com.trailguide.android.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trailguide.android.BuildConfig
import com.trailguide.android.data.model.Language
import com.trailguide.android.presentation.theme.*
import com.trailguide.android.presentation.viewmodel.ProfileViewModel

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
    val currentUser by viewModel.currentUser.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
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
                        Icon(Icons.Default.Close, "Dismiss", tint = Error)
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
                        Icon(Icons.Default.Close, "Dismiss", tint = Success)
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
                    Text("Authentication (Supabase)", style = MaterialTheme.typography.titleMedium)
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
                            text = if (isSignedIn) currentUser?.email ?: "User" else "Guest",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Supabase Auth",
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
                            Text("Sign In")
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.signOut() }) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign Out")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Info text about Supabase OAuth
                Text(
                    "Sign in with Google using Supabase Authentication",
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
                        "Or sign in with Email",
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
                        label = { Text("Email") },
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
                        label = { Text("Password") },
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
                            label = { Text("Display Name") },
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
                            Text(if (isRegistering) "Register" else "Sign In")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                isRegistering = !isRegistering
                                displayName = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isRegistering) "Back to Sign In" else "Register")
                        }
                    }
                    
                    if (!isRegistering) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Note: If you signed in with Google, use Google sign-in above",
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
                    Text("Biometric Login", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable biometric authentication", color = TextSecondary)
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
                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trail reminders & safety alerts", color = TextSecondary)
                    Switch(
                        checked = userPreferences.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                }
            }
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
                    Text("Language", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Language.entries.forEach { language ->
                        FilterChip(
                            selected = userPreferences.language == language,
                            onClick = { viewModel.setLanguage(language) },
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
                Text("TrailGuide Android", style = MaterialTheme.typography.titleMedium)
                Text("Version ${BuildConfig.VERSION_NAME}", color = TextSecondary)
                Text("Native Kotlin • MVVM Architecture", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text("Supabase Authentication & Database", color = Primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
