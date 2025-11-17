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
    
    // Default location: Magaliesberg, South Africa (only used as fallback)
    val defaultLocation = LatLng(-25.792, 27.946)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }
    
    var mapType by remember { mutableStateOf(MapType.TERRAIN) }
    var selectedTrailId by remember { mutableStateOf<String?>(null) }
    var hasLoadedInitialTrails by remember { mutableStateOf(false) }
    
    // State from hybrid MapViewModel
    val googlePlacesTrails by mapViewModel.googlePlacesTrails.collectAsState()
    val osmTrails by mapViewModel.osmTrails.collectAsState()
    val selectedGoogleTrail by mapViewModel.selectedGoogleTrail.collectAsState()
    val selectedOsmTrail by mapViewModel.selectedOsmTrail.collectAsState()
    val isLoadingGoogleTrails by mapViewModel.isLoadingGoogleTrails.collectAsState()
    val isLoadingOsmTrails by mapViewModel.isLoadingOsmTrails.collectAsState()
    val googleError by mapViewModel.googleError.collectAsState()
    val osmError by mapViewModel.osmError.collectAsState()
    val showOsmTrails by mapViewModel.showOsmTrails.collectAsState()
    val showGooglePlaces by mapViewModel.showGooglePlaces.collectAsState()
    
    // DO NOT automatically load trails on startup
    // User must explicitly click "Refresh" or location button to load trails
    
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
            // OSM Hiking Trails (Real trail geometry)
            if (showOsmTrails) {
                osmTrails.forEach { osmTrail ->
                    val isSelected = selectedOsmTrail?.id == osmTrail.id
                    Polyline(
                        points = osmTrail.geometry,
                        color = if (isSelected) Color(0xFFFF6B35) else Color(0xFF4CAF50),
                        width = if (isSelected) 12f else 8f,
                        clickable = true,
                        onClick = {
                            mapViewModel.selectOsmTrail(osmTrail)
                            android.util.Log.d("MapScreen", "Selected OSM trail: ${osmTrail.name}")
                            true
                        }
                    )
                    
                    // Start point marker for OSM trail
                    osmTrail.startPoint?.let { start ->
                        Marker(
                            state = MarkerState(position = start),
                            title = osmTrail.name,
                            snippet = "${osmTrail.trailType.displayName} • ${String.format("%.2f", osmTrail.distance / 1000)} km",
                            onClick = {
                                mapViewModel.selectOsmTrail(osmTrail)
                                true
                            }
                        )
                    }
                }
            }
            // Add markers for trails from backend/Google Places
            trails.forEach { trail ->
                val isSelected = selectedTrailId == trail.id
                
                // Only show START marker for the trail
                Marker(
                    state = MarkerState(position = LatLng(trail.latitude, trail.longitude)),
                    title = trail.name,
                    snippet = "${trail.difficulty?.displayName ?: "Moderate"} • ${trail.distanceKm} km • Tap to load trail details",
                    onClick = {
                        selectedTrailId = if (isSelected) null else trail.id
                        // When trail is clicked, search for its OSM geometry
                        if (!isSelected) {
                            android.util.Log.d("MapScreen", "Trail clicked: ${trail.name}, searching for OSM data...")
                            mapViewModel.searchOsmTrailByName(
                                trailName = trail.name,
                                latitude = trail.latitude,
                                longitude = trail.longitude,
                                radius = 5000 // 5km search radius
                            )
                        }
                        true
                    }
                )
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
                
                // Show loading indicator when fetching OSM geometry
                if (isLoadingOsmTrails) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Loading trail geometry...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (osmTrails.isEmpty()) {
                    Text(
                        "Tap a trail marker to load detailed route",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // Error messages
                if (osmError != null) {
                    Text(
                        "OSM Error: $osmError",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (googleError != null) {
                    Text(
                        "Google Error: $googleError",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // Selected OSM Trail Info
                selectedOsmTrail?.let { trail ->
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(
                        trail.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${trail.trailType.displayName} • ${trail.difficulty.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Distance: ${String.format("%.2f", trail.distance / 1000)} km",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { 
                            // TODO: Start navigation with this OSM trail
                            android.util.Log.d("MapScreen", "Start navigation with ${trail.name}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Navigation")
                    }
                    
                    TextButton(
                        onClick = { 
                            mapViewModel.clearSelection()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Selection")
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
                            "${trail.difficulty?.displayName ?: "Moderate"} • ${trail.distanceKm} km • ${trail.elevationM}m elevation",
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
