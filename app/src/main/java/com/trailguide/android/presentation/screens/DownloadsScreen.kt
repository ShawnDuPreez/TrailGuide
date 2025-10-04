package com.trailguide.android.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trailguide.android.data.model.Trail
import com.trailguide.android.presentation.theme.*
import com.trailguide.android.presentation.viewmodel.DownloadsViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Downloads screen showing offline trail packs.
 * Displays downloaded trails and allows managing offline content.
 */
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onTrailClick: (String) -> Unit = {}
) {
    val downloadedTrails by viewModel.downloadedTrails.collectAsState()
    val storageUsedBytes by viewModel.storageUsedBytes.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    
    val storageUsed = viewModel.formatStorageSize(storageUsedBytes)
    val storageTotal = "500 MB"
    val storageProgress = (storageUsedBytes / (500.0 * 1024 * 1024)).toFloat().coerceIn(0f, 1f)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Error message
        errorMessage?.let { error ->
            Card(colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = error, modifier = Modifier.weight(1f), color = Error)
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(Icons.Default.Close, "Dismiss", tint = Error)
                    }
                }
            }
        }
        
        // Success message
        successMessage?.let { success ->
            Card(colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f))) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = success, modifier = Modifier.weight(1f), color = Primary)
                    IconButton(onClick = { viewModel.clearSuccess() }) {
                        Icon(Icons.Default.Close, "Dismiss", tint = Primary)
                    }
                }
            }
        }
        // Storage usage card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Storage Used",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "$storageUsed of $storageTotal",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { showDeleteAllDialog = true },
                        enabled = downloadedTrails.isNotEmpty() && !isLoading
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = { storageProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        
        // Downloaded trails section
        Text(
            "Offline Packs",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        if (downloadedTrails.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Text(
                        "No offline trails yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Download trails for offline access",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            // Downloaded trails list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloadedTrails) { trail ->
                    DownloadedTrailCard(
                        trail = trail,
                        onClick = { onTrailClick(trail.id) },
                        onDelete = { viewModel.deleteDownload(trail.id, trail.name) }
                    )
                }
            }
        }
    }
    
    // Delete all confirmation dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Clear All Downloads?") },
            text = { Text("This will remove all ${downloadedTrails.size} downloaded trails from your device. You can download them again later.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllDownloads()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Clear All", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Card component for a downloaded trail.
 */
@Composable
fun DownloadedTrailCard(
    trail: Trail,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val downloadedDate = remember { "Downloaded" } // You can calculate relative time if needed
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.DownloadDone,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        trail.name,
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    // Route availability indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (trail.routeCoordinates.isNotEmpty()) {
                            Icon(
                                Icons.Default.Route,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Primary
                            )
                            Text(
                                "Route available • ${trail.routeCoordinates.size} points",
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary
                            )
                        } else {
                            Text(
                                "Marker only",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${trail.distanceKm} km",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                        Text("•", color = TextTertiary)
                        Text(
                            trail.difficulty.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                        Text("•", color = TextTertiary)
                        Text(
                            downloadedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = AccentRed
                )
            }
        }
    }
}

