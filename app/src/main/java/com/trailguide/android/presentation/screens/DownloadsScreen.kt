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
import com.trailguide.android.presentation.theme.*

/**
 * Downloads screen showing offline trail packs.
 * Displays downloaded trails and allows managing offline content.
 */
@Composable
fun DownloadsScreen() {
    // Mock downloaded trails data
    val downloadedTrails = remember {
        listOf(
            DownloadedTrail(
                "Mount Lion Ridge",
                "Map + GPS data",
                "45.2 MB",
                "2 days ago"
            ),
            DownloadedTrail(
                "Cedar Valley Loop",
                "Map + GPS data",
                "28.7 MB",
                "1 week ago"
            )
        )
    }
    
    val storageUsed = "73.9 MB"
    val storageTotal = "500 MB"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    
                    OutlinedButton(onClick = { /* Clear all downloads */ }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = 0.148f, // 73.9 / 500
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary
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
                    DownloadedTrailCard(trail)
                }
            }
        }
    }
}

/**
 * Data class for downloaded trail information.
 */
data class DownloadedTrail(
    val name: String,
    val content: String,
    val size: String,
    val downloadedDate: String
)

/**
 * Card component for a downloaded trail.
 */
@Composable
fun DownloadedTrailCard(trail: DownloadedTrail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                
                Column {
                    Text(
                        trail.name,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        trail.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            trail.size,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                        Text("•", color = TextTertiary)
                        Text(
                            trail.downloadedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }
            
            IconButton(onClick = { /* Remove download */ }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = AccentRed
                )
            }
        }
    }
}

