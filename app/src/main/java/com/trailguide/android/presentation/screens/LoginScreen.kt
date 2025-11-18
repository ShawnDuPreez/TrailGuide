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
    // Make hasBiometricCredentials reactive - check it dynamically instead of using remember
    var hasBiometricCredentials by remember { mutableStateOf(viewModel.hasBiometricCredentials()) }
    
    // Refresh hasBiometricCredentials when the screen is visible or when credentials might have changed
    LaunchedEffect(Unit) {
        // Check credentials periodically or when screen becomes visible
        hasBiometricCredentials = viewModel.hasBiometricCredentials()
    }
    
    // Also check when user might have enabled biometric login (e.g., after returning from settings)
    DisposableEffect(Unit) {
        val checkCredentials = {
            hasBiometricCredentials = viewModel.hasBiometricCredentials()
        }
        // Check immediately and set up a way to refresh
        checkCredentials()
        onDispose { }
    }
    
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
            onValueChange = { viewModel.setEmail(it) },
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
        
        // Show biometric dialog after successful login (for email/password users)
        LaunchedEffect(authenticatedUser) {
            authenticatedUser?.let { user ->
                // Only show dialog for email/password users (SSO users can enable in settings)
                if (isBiometricAvailable && !hasBiometricCredentials && user.provider == com.trailguide.android.data.model.AuthProvider.EMAIL) {
                    showBiometricDialog = true
                }
            }
        }
        
        // Biometric login button (if available and credentials stored)
        // Check credentials dynamically when button is clicked
        if (isBiometricAvailable) {
            // Check credentials each time this composable recomposes
            val currentHasCredentials = viewModel.hasBiometricCredentials()
            if (currentHasCredentials || hasBiometricCredentials) {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { 
                        if (context is FragmentActivity) {
                            // Refresh credentials check before attempting login
                            hasBiometricCredentials = viewModel.hasBiometricCredentials()
                            if (hasBiometricCredentials) {
                                viewModel.loginWithBiometric(context)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Login with Biometric")
                }
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
            title = { Text("Enable Biometric Login") },
            text = { 
                Text("Would you like to enable biometric login for faster access? Your credentials will be stored securely on this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (context is FragmentActivity) {
                            viewModel.storeBiometricCredentials(context)
                        }
                        showBiometricDialog = false
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBiometricDialog = false }
                ) {
                    Text("Not Now")
                }
            }
        )
    }
}

