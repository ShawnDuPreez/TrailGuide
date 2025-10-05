package com.trailguide.android.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.compose.*
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.presentation.theme.Primary
import com.trailguide.android.presentation.viewmodel.MapViewModel
import com.trailguide.android.presentation.viewmodel.TrailsViewModel

/**
 * Map screen displaying trail locations using Google Maps.
 * Shows trails as markers with routes (polylines) when available.
 */
@Composable
fun MapScreen(
    viewModel: TrailsViewModel = hiltViewModel(),
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val trails by viewModel.trails.collectAsState()
    val context = LocalContext.current
    
    // State for location permission
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }
    
    // State for showing permission rationale
    var showPermissionRationale by remember { mutableStateOf(false) }
    
    // Request location permission when screen loads
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            // Check if we should show rationale first
            val shouldShowRationale = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            
            if (shouldShowRationale) {
                showPermissionRationale = true
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
    
    // Default location: Magaliesberg, South Africa
    val defaultLocation = LatLng(-25.792, 27.946)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }
    
    var mapType by remember { mutableStateOf(MapType.TERRAIN) }
    var selectedTrailId by remember { mutableStateOf<String?>(null) }
    
    // OSM Trail state from ViewModel
    val osmTrails by mapViewModel.osmTrails.collectAsState()
    val isLoadingOsmTrails by mapViewModel.isLoadingOsmTrails.collectAsState()
    val osmTrailError by mapViewModel.osmTrailError.collectAsState()
    val showOsmTrails by mapViewModel.showOsmTrails.collectAsState()
    val selectedTrail by mapViewModel.selectedTrail.collectAsState()
    val availableTrails by mapViewModel.availableTrails.collectAsState()
    
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
            // OSM Hiking Trails
            if (showOsmTrails) {
                osmTrails.forEachIndexed { index, polylineOptions ->
                    Polyline(
                        points = polylineOptions.points,
                        color = Color(polylineOptions.color),
                        width = polylineOptions.width,
                        clickable = true,
                        onClick = { polyline ->
                            // Handle polyline click - show trail info
                            android.util.Log.d("MapScreen", "Clicked OSM trail #$index")
                            // You can show an info window or dialog here
                            true
                        }
                    )
                }
            }
            
            // Selected Trail (when user starts a hike)
            selectedTrail?.let { trail ->
                Polyline(
                    points = trail.points,
                    color = Color(trail.color),
                    width = trail.width + 2f, // Make selected trail slightly thicker
                    clickable = true,
                    onClick = { polyline ->
                        android.util.Log.d("MapScreen", "Clicked selected trail")
                        true
                    }
                )
            }
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
        
        // Map type selector and OSM trails toggle
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    "Map Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                
                Text(
                    "OSM Trails",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (isLoadingOsmTrails) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Loading...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    TextButton(
                        onClick = { mapViewModel.toggleOsmTrails() }
                    ) {
                        Icon(
                            if (showOsmTrails) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (showOsmTrails) "Hide OSM" else "Show OSM",
                            color = if (showOsmTrails) Primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                if (osmTrailError != null) {
                    Text(
                        "Error: ${osmTrailError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // Trail Selector
                if (availableTrails.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(
                        "Start Hike",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    availableTrails.take(3).forEach { (trailId, trailName) ->
                        TextButton(
                            onClick = { 
                                mapViewModel.selectTrail(trailId)
                                selectedTrailId = trailId
                            }
                        ) {
                            Text(
                                trailName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedTrailId == trailId) Primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    if (selectedTrailId != null) {
                        TextButton(
                            onClick = { 
                                mapViewModel.clearSelectedTrail()
                                selectedTrailId = null
                            }
                        ) {
                            Text(
                                "Clear Selection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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
                    
                    if (showOsmTrails && osmTrails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "OSM Trails: ${osmTrails.size} hiking paths loaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary
                        )
                    }
                }
            }
        }
        
        // Permission rationale dialog
        if (showPermissionRationale) {
            AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                title = { Text("Location Permission Needed") },
                text = { 
                    Text("TrailGuide needs access to your location to show your current position on the map and help you navigate to nearby trails. This makes your hiking experience safer and more convenient.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPermissionRationale = false
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    ) {
                        Text("Allow")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPermissionRationale = false }
                    ) {
                        Text("Skip")
                    }
                }
            )
        }
    }
}
