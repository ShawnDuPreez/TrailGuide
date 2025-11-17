package com.trailguide.android.presentation.screens

import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trailguide.android.presentation.theme.Primary
import com.trailguide.android.presentation.viewmodel.AuthViewModel

/**
 * Login screen with email/password authentication
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (com.trailguide.android.data.model.User) -> Unit
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val authenticatedUser by viewModel.authenticatedUser.collectAsState()
    
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var showBiometricDialog by remember { mutableStateOf(false) }
    
    // Check biometric availability
    val isBiometricAvailable = remember { viewModel.isBiometricAvailable() }
    val hasBiometricCredentials = remember { viewModel.hasBiometricCredentials() }
    val hasBiometricForEmail by viewModel.hasBiometricForEmail.collectAsState()
    
    // Observe login success
    LaunchedEffect(authenticatedUser) {
        authenticatedUser?.let { user ->
            onLoginSuccess(user)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo/Icon
        Icon(
            Icons.Default.Hiking,
            contentDescription = "TrailGuide Logo",
            modifier = Modifier.size(80.dp),
            tint = Primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Login",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            "Welcome back to TrailGuide",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { 
                viewModel.setEmail(it)
                // Check biometric availability for this email
                if (it.isNotBlank()) {
                    viewModel.checkBiometricForEmail(it)
                }
            },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        // Check biometric when email changes
        LaunchedEffect(email) {
            if (email.isNotBlank()) {
                viewModel.checkBiometricForEmail(email)
            } else {
                viewModel.checkBiometricForEmail("")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.setPassword(it) },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { 
                    focusManager.clearFocus()
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.login()
                    }
                }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Sign In button
        Button(
            onClick = { 
                viewModel.login()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In")
            }
        }
        
        // Biometric login button (show if biometric is available and email has credentials)
        if (isBiometricAvailable && email.isNotBlank() && hasBiometricForEmail) {
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { 
                    // Authenticate with stored credentials for this email
                    if (context is FragmentActivity) {
                        viewModel.loginWithBiometric(context, email)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log in with biometrics")
            }
        }
        
        // Show setup button if biometric is available but no credentials for this email
        if (isBiometricAvailable && !hasBiometricForEmail && email.isNotBlank() && password.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { 
                    // Show dialog to set up biometric
                    showBiometricDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set up Biometric Login")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                "  or  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Divider(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Google SSO Login button
        OutlinedButton(
            onClick = { viewModel.loginWithGoogle() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue with Google")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Create Account button
        OutlinedButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Account")
        }
        
    }
    
    // Biometric Enablement Dialog
    if (showBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricDialog = false },
            title = { Text("Set up Biometric Login") },
            text = { 
                if (hasBiometricCredentials) {
                    Text("Biometric login is already set up. You can use your fingerprint or face to sign in.")
                } else if (email.isNotBlank() && password.isNotBlank()) {
                    Text("Would you like to enable biometric login for faster access? Your credentials will be stored securely on this device using ${if (isBiometricAvailable) "fingerprint or face authentication" else "biometric authentication"}.")
                } else {
                    Text("To set up biometric login, please enter your email and password first, then sign in successfully. After that, you'll be able to enable biometric authentication.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank() && !hasBiometricCredentials) {
                            if (context is FragmentActivity) {
                                viewModel.storeBiometricCredentials(context)
                            }
                        }
                        showBiometricDialog = false
                    },
                    enabled = email.isNotBlank() && password.isNotBlank() && !hasBiometricCredentials
                ) {
                    Text(if (hasBiometricCredentials) "OK" else "Enable")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBiometricDialog = false }
                ) {
                    Text(if (email.isNotBlank() && password.isNotBlank() && !hasBiometricCredentials) "Not Now" else "Close")
                }
            }
        )
    }
    
    // Show biometric setup dialog after successful login
    LaunchedEffect(authenticatedUser, isBiometricAvailable, hasBiometricCredentials) {
        if (authenticatedUser != null && isBiometricAvailable && !hasBiometricCredentials && email.isNotBlank() && password.isNotBlank()) {
            showBiometricDialog = true
        }
    }
}

