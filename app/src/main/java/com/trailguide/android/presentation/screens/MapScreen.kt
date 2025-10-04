package com.trailguide.android.presentation.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.presentation.theme.Primary
import com.trailguide.android.presentation.viewmodel.TrailsViewModel

/**
 * Map screen displaying trail locations using Google Maps.
 * Shows trails as markers with routes (polylines) when available.
 */
@Composable
fun MapScreen(
    viewModel: TrailsViewModel = hiltViewModel()
) {
    val trails by viewModel.trails.collectAsState()
    val context = LocalContext.current
    
    // Check if we have location permission
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
    }
    
    // Default location: Magaliesberg, South Africa
    val defaultLocation = LatLng(-25.792, 27.946)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }
    
    var mapType by remember { mutableStateOf(MapType.TERRAIN) }
    var selectedTrailId by remember { mutableStateOf<String?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true,
                myLocationButtonEnabled = hasLocationPermission,
                mapToolbarEnabled = true
            )
        ) {
            // Add markers and routes for all trails
            trails.forEach { trail ->
                val isSelected = selectedTrailId == trail.id
                
                // Marker for trail start point
                Marker(
                    state = MarkerState(position = LatLng(trail.latitude, trail.longitude)),
                    title = trail.name,
                    snippet = "${trail.difficulty.displayName} • ${trail.distanceKm} km",
                    onClick = {
                        selectedTrailId = if (isSelected) null else trail.id
                        true
                    }
                )
                
                // Draw route if available
                if (trail.routeCoordinates.isNotEmpty()) {
                    val routePoints = trail.routeCoordinates.map { 
                        LatLng(it.latitude, it.longitude) 
                    }
                    
                    Polyline(
                        points = routePoints,
                        color = when (trail.difficulty) {
                            Difficulty.EASY -> Color(0xFF4CAF50) // Green
                            Difficulty.MODERATE -> Color(0xFFFFA726) // Orange
                            Difficulty.HARD -> Color(0xFFEF5350) // Red
                        },
                        width = if (isSelected) 12f else 8f,
                        visible = true,
                        clickable = true,
                        onClick = {
                            selectedTrailId = if (isSelected) null else trail.id
                        }
                    )
                }
            }
        }
        
        // Map type selector
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                TextButton(onClick = { mapType = MapType.NORMAL }) {
                    Text(
                        "Normal",
                        color = if (mapType == MapType.NORMAL) Primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = { mapType = MapType.SATELLITE }) {
                    Text(
                        "Satellite",
                        color = if (mapType == MapType.SATELLITE) Primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = { mapType = MapType.TERRAIN }) {
                    Text(
                        "Terrain",
                        color = if (mapType == MapType.TERRAIN) Primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        // Selected trail info card
        selectedTrailId?.let { trailId ->
            val selectedTrail = trails.find { it.id == trailId }
            selectedTrail?.let { trail ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            trail.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${trail.difficulty.displayName} • ${trail.distanceKm} km • ${trail.elevationM}m elevation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (trail.routeCoordinates.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "✓ Route available (${trail.routeCoordinates.size} points)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "ℹ Route not available - marker only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Info card when no trail selected
        if (selectedTrailId == null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Trail Map",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Tap markers or routes to view trail details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (trails.any { it.routeCoordinates.isNotEmpty() }) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🟢 Easy", style = MaterialTheme.typography.bodySmall)
                            Text("🟠 Moderate", style = MaterialTheme.typography.bodySmall)
                            Text("🔴 Hard", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
