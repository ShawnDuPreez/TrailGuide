package com.trailguide.android.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.data.model.Trail
import com.trailguide.android.presentation.theme.*
import com.trailguide.android.presentation.viewmodel.TrailsViewModel

/**
 * Trails list screen with search and filters.
 * Displays a list of hiking trails that can be filtered by difficulty and distance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailsScreen(
    viewModel: TrailsViewModel = hiltViewModel(),
    onTrailClick: (String) -> Unit
) {
    val trails by viewModel.filteredTrails.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val maxDistance by viewModel.maxDistance.collectAsState()
    val maxProximity by viewModel.maxProximity.collectAsState()
    val maxDuration by viewModel.maxDuration.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var showFilters by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search trails...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(
                        if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle filters"
                    )
                }
            },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Filters panel (expandable)
        if (showFilters) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Difficulty", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedDifficulty == null,
                            onClick = { viewModel.setDifficulty(null) },
                            label = { Text("Any") }
                        )
                        FilterChip(
                            selected = selectedDifficulty == Difficulty.EASY,
                            onClick = { viewModel.setDifficulty(Difficulty.EASY) },
                            label = { Text("Easy") }
                        )
                        FilterChip(
                            selected = selectedDifficulty == Difficulty.MODERATE,
                            onClick = { viewModel.setDifficulty(Difficulty.MODERATE) },
                            label = { Text("Moderate") }
                        )
                        FilterChip(
                            selected = selectedDifficulty == Difficulty.HARD,
                            onClick = { viewModel.setDifficulty(Difficulty.HARD) },
                            label = { Text("Hard") }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Trail Distance: ≤ ${maxDistance.toInt()} km", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = maxDistance.toFloat(),
                        onValueChange = { viewModel.setMaxDistance(it.toDouble()) },
                        valueRange = 1f..30f,
                        steps = 29
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Proximity from me: ${maxProximity?.toInt()?.toString() ?: "Any"} km", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = maxProximity != null,
                            onCheckedChange = { checked ->
                                viewModel.setMaxProximity(if (checked) 10.0 else null)
                            }
                        )
                        Text("Enable proximity filter")
                    }
                    if (maxProximity != null) {
                        Slider(
                            value = maxProximity!!.toFloat(),
                            onValueChange = { viewModel.setMaxProximity(it.toDouble()) },
                            valueRange = 1f..50f,
                            steps = 49
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Max Duration: ${maxDuration?.let { "%.1f hrs".format(it) } ?: "Any"}", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = maxDuration != null,
                            onCheckedChange = { checked ->
                                viewModel.setMaxDuration(if (checked) 4.0 else null)
                            }
                        )
                        Text("Enable duration filter")
                    }
                    if (maxDuration != null) {
                        Slider(
                            value = maxDuration!!.toFloat(),
                            onValueChange = { viewModel.setMaxDuration(it.toDouble()) },
                            valueRange = 0.5f..10f,
                            steps = 19
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.clearFilters() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear All Filters")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = Error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Trails list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(trails, key = { it.id }) { trail ->
                TrailCard(
                    trail = trail,
                    onClick = { onTrailClick(trail.id) },
                    onFavoriteClick = { viewModel.toggleFavorite(trail.id, !trail.isFavorite) }
                )
            }
        }
    }
}

/**
 * Trail card component displaying trail information.
 */
@Composable
fun TrailCard(
    trail: Trail,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            // Trail image
            AsyncImage(
                model = trail.imageUrl,
                contentDescription = trail.name,
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            
            // Trail info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trail.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    DifficultyBadge(difficulty = trail.difficulty)
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = trail.city,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TrailStat(Icons.Default.Terrain, "${trail.distanceKm} km")
                        TrailStat(Icons.Default.TrendingUp, "${trail.elevationM} m")
                        TrailStat(Icons.Default.Star, trail.rating.toString())
                    }
                    
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (trail.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (trail.isFavorite) AccentRed else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Difficulty badge component.
 */
@Composable
fun DifficultyBadge(difficulty: Difficulty) {
    val color = when (difficulty) {
        Difficulty.EASY -> DifficultyEasy
        Difficulty.MODERATE -> DifficultyModerate
        Difficulty.HARD -> DifficultyHard
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = difficulty.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Trail stat component (icon + value).
 */
@Composable
fun TrailStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

