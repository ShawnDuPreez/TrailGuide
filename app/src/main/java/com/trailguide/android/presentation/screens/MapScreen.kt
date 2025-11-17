package com.trailguide.android.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
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
    
    // State for expandable controls
    var showMapControls by remember { mutableStateOf(false) }
    
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
    
    // Boundary states
    val selectedBoundary by mapViewModel.selectedBoundary.collectAsState()
    val boundaryTrails by mapViewModel.boundaryTrails.collectAsState()
    val isLoadingBoundary by mapViewModel.isLoadingBoundary.collectAsState()
    val boundaryError by mapViewModel.boundaryError.collectAsState()
    val combinedTrailStats by mapViewModel.combinedTrailStats.collectAsState()
    
    // Download state
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
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
            // Render OSM boundary polygon (if available)
            selectedBoundary?.let { boundary ->
                if (boundary.polygon.isNotEmpty()) {
                    Polygon(
                        points = boundary.polygon,
                        strokeColor = Color.Blue.copy(alpha = 0.8f),
                        strokeWidth = 5f,
                        fillColor = Color.Blue.copy(alpha = 0.1f),
                        clickable = false
                    )
                }
            }
            
            // OSM Hiking Trails (Real trail geometry)
            // Show ALL trails within the selected boundary
            if (showOsmTrails) {
                osmTrails.forEach { osmTrail ->
                    val isSelected = selectedOsmTrail?.id == osmTrail.id
                    
                    // Draw the trail polyline
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
                }
            }
            // Add markers for trails from backend/Google Places
            trails.forEach { trail ->
                val isSelected = selectedTrailId == trail.id
                
                // Only show START marker for the trail
                Marker(
                    state = MarkerState(position = LatLng(trail.latitude, trail.longitude)),
                    title = trail.name,
                    snippet = "${trail.difficulty?.displayName ?: "Moderate"} • ${trail.distanceKm} km",
                    onClick = {
                        selectedTrailId = if (isSelected) null else trail.id
                        // When trail is clicked, load OSM trails WITHIN that location's boundary
                        // This shows real hiking paths inside the nature reserve/park
                        if (!isSelected) {
                            android.util.Log.d("MapScreen", "Trail clicked: ${trail.name} - Loading boundary trails")
                            // Use the new boundary-based loading
                            mapViewModel.loadTrailsForGooglePlace(
                                placeName = trail.name,
                                latitude = trail.latitude,
                                longitude = trail.longitude
                            )
                        } else {
                            // Clear OSM trails and boundary when deselecting
                            mapViewModel.clearOsmTrails()
                            mapViewModel.clearBoundary()
                        }
                        true
                    }
                )
            }
        }
        
        // Floating Action Button for map controls (top-right corner)
        FloatingActionButton(
            onClick = { showMapControls = !showMapControls },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = if (showMapControls) Icons.Default.Close else Icons.Default.Settings,
                contentDescription = if (showMapControls) "Close controls" else "Map controls"
            )
        }
        
        // Expandable map controls panel
        androidx.compose.animation.AnimatedVisibility(
            visible = showMapControls,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp),
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
        ) {
            Card(
                modifier = Modifier.width(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Map Type Section
                    Text(
                        "Map Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = mapType == MapType.NORMAL,
                            onClick = { mapType = MapType.NORMAL },
                            label = { Text("Map", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = mapType == MapType.SATELLITE,
                            onClick = { mapType = MapType.SATELLITE },
                            label = { Text("Sat", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = mapType == MapType.TERRAIN,
                        onClick = { mapType = MapType.TERRAIN },
                        label = { Text("Terrain", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Divider()
                    
                    // OSM Trails Toggle
                    Text(
                        "Trail Layers",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "OSM Trails",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Switch(
                            checked = showOsmTrails,
                            onCheckedChange = { mapViewModel.toggleOsmTrails() },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Google Places",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Switch(
                            checked = showGooglePlaces,
                            onCheckedChange = { mapViewModel.toggleGooglePlaces() },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    
                    // Boundary info (if loaded)
                    selectedBoundary?.let { boundary ->
                        Divider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                "Current Area:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                boundary.name.take(30) + if (boundary.name.length > 30) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                maxLines = 2
                            )
                            Text(
                                "${boundaryTrails.size} trails",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Loading indicator
                    if (isLoadingBoundary || isLoadingOsmTrails) {
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isLoadingBoundary) "Finding area..." else "Loading trails...",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    // Download button for boundary areas
                    if (!isLoadingBoundary && !isDownloading && boundaryTrails.isNotEmpty()) {
                        Divider()
                        Text(
                            "Offline Maps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        
                        combinedTrailStats?.let { stats ->
                            Text(
                                "${stats.trailCount} trails • ${String.format("%.1f", stats.totalDistance / 1000)} km • ${String.format("%.0f", stats.totalElevation)} m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Button(
                            onClick = { showDownloadDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Area")
                        }
                    }
                }
            }
        }
        
        // Compact status indicator (bottom-right, always visible if boundary loaded)
        selectedBoundary?.let { boundary ->
            if (!showMapControls) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .clickable { showMapControls = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                boundary.name.take(25) + if (boundary.name.length > 25) "..." else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                "${boundaryTrails.size} trails",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        
        // Download confirmation dialog
        if (showDownloadDialog) {
            selectedBoundary?.let { boundary ->
                combinedTrailStats?.let { stats ->
                    AlertDialog(
                        onDismissRequest = { showDownloadDialog = false },
                        title = { Text("Download Offline Area") },
                        text = {
                            Column {
                                Text("Download ${boundary.name} for offline use?")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "• ${stats.trailCount} trails\n• ${String.format("%.1f", stats.totalDistance / 1000)} km total distance\n• ${String.format("%.0f", stats.totalElevation)} m elevation gain",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDownloadDialog = false
                                    isDownloading = true
                                    downloadProgress = 0f
                                    downloadMessage = "Starting download..."
                                    
                                    // Start download in coroutine
                                    coroutineScope.launch {
                                        mapViewModel.downloadCurrentArea()?.collect { progress ->
                                            downloadProgress = progress.progress
                                            downloadMessage = progress.message
                                            if (progress.progress >= 1.0f) {
                                                isDownloading = false
                                                downloadMessage = "Download complete! Check Downloads section."
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Download")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDownloadDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
        
        // Download progress dialog
        if (isDownloading) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Downloading...") },
                text = {
                    Column {
                        Text(downloadMessage)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = downloadProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    if (downloadProgress >= 1.0f) {
                        TextButton(
                            onClick = { isDownloading = false }
                        ) {
                            Text("Done")
                        }
                    }
                }
            )
        }
    }
}
