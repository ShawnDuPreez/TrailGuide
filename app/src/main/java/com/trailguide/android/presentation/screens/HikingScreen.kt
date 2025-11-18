package com.trailguide.android.presentation.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.trailguide.android.data.model.Difficulty
import com.trailguide.android.data.model.RoutePoint
import com.trailguide.android.data.model.Trail
import com.trailguide.android.presentation.theme.*
import com.trailguide.android.presentation.viewmodel.HikingViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * Hiking screen with GPS navigation, timer, and elevation tracking.
 * Similar to Google Maps navigation but designed for hiking.
 * Reference: Platform, G. M., 2025. Maps SDK for Android Quickstart. https://developers.google.com/maps/documentation/android-sdk/start
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikingScreen(
    trail: Trail,
    onNavigateBack: () -> Unit,
    viewModel: HikingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isHiking by viewModel.isHiking.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val currentElevation by viewModel.currentElevation.collectAsState()
    val distanceTraveled by viewModel.distanceTraveled.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val progress by viewModel.progress.collectAsState()
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.requestLocationPermission(context)
        }
    }
    
    // Initialize trail and request location permission
    LaunchedEffect(Unit) {
        viewModel.setTrail(trail)
        
        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.requestLocationPermission(context)
        } else {
            // Request permission
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        trail.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // Map background
            HikingMap(
                trail = trail,
                currentLocation = currentLocation,
                progress = progress,
                modifier = Modifier.fillMaxSize()
            )
            
            // Top stats overlay
            HikingStats(
                elapsedTime = elapsedTime,
                currentElevation = currentElevation,
                distanceTraveled = distanceTraveled,
                progress = progress,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
            
            // Bottom controls
            HikingControls(
                isHiking = isHiking,
                onStartHike = { viewModel.startHike() },
                onPauseHike = { viewModel.pauseHike() },
                onResumeHike = { viewModel.resumeHike() },
                onStopHike = { viewModel.stopHike() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
            
            // Progress indicator
            if (trail.routeCoordinates.isNotEmpty()) {
                TrailProgress(
                    progress = progress,
                    trail = trail,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * Map component showing the trail route and current position.
 */
@Composable
fun HikingMap(
    trail: Trail,
    currentLocation: LatLng?,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(trail.latitude, trail.longitude),
            16f
        )
    }
    
    // Update camera when location changes
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(location, 16f)
                ),
                durationMs = 1000
            )
        }
    }
    
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = true,
            mapType = MapType.TERRAIN
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        )
    ) {
        // Trail route
        if (trail.routeCoordinates.isNotEmpty()) {
            val routePoints = trail.routeCoordinates.map { 
                LatLng(it.latitude, it.longitude) 
            }
            
            // Completed portion (green)
            val completedPoints = routePoints.take((routePoints.size * progress).toInt())
            if (completedPoints.size > 1) {
                Polyline(
                    points = completedPoints,
                    color = Color(0xFF4CAF50),
                    width = 12f
                )
            }
            
            // Remaining portion (gray)
            val remainingPoints = routePoints.drop((routePoints.size * progress).toInt())
            if (remainingPoints.size > 1) {
                Polyline(
                    points = remainingPoints,
                    color = Color.Gray,
                    width = 8f
                )
            }
        }
        
        // Start marker
        Marker(
            state = MarkerState(position = LatLng(trail.latitude, trail.longitude)),
            title = "Start",
            snippet = trail.name
        )
        
        // End marker
        if (trail.routeCoordinates.isNotEmpty()) {
            val endPoint = trail.routeCoordinates.last()
            Marker(
                state = MarkerState(position = LatLng(endPoint.latitude, endPoint.longitude)),
                title = "End",
                snippet = "Trail finish"
            )
        }
    }
}

/**
 * Stats overlay showing time, elevation, and distance.
 */
@Composable
fun HikingStats(
    elapsedTime: Long,
    currentElevation: Double,
    distanceTraveled: Double,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                icon = Icons.Default.Timer,
                value = formatTime(elapsedTime),
                label = "Time",
                color = Primary
            )
            
            StatCard(
                icon = Icons.Default.Terrain,
                value = "${currentElevation.toInt()}m",
                label = "Elevation",
                color = AccentBlue
            )
            
            StatCard(
                icon = Icons.Default.Speed,
                value = "${distanceTraveled.toInt()}m",
                label = "Distance",
                color = Color(0xFF4CAF50)
            )
            
            StatCard(
                icon = Icons.Default.TrendingUp,
                value = "${(progress * 100).toInt()}%",
                label = "Progress",
                color = Color(0xFFFFA726)
            )
        }
    }
}

/**
 * Individual stat card component.
 */
@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

/**
 * Bottom controls for hiking actions.
 */
@Composable
fun HikingControls(
    isHiking: Boolean,
    onStartHike: () -> Unit,
    onPauseHike: () -> Unit,
    onResumeHike: () -> Unit,
    onStopHike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                !isHiking -> {
                    // Start button
                    Button(
                        onClick = onStartHike,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Hike")
                    }
                }
                isHiking -> {
                    // Pause button
                    OutlinedButton(
                        onClick = onPauseHike,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pause")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Stop button
                    OutlinedButton(
                        onClick = onStopHike,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF5350)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                }
            }
        }
    }
}

/**
 * Trail progress indicator on the side.
 */
@Composable
fun TrailProgress(
    progress: Float,
    trail: Trail,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "progress"
    )
    
    Card(
        modifier = modifier.width(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Trail",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(200.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedProgress)
                        .background(
                            when (trail.difficulty) {
                                Difficulty.EASY -> Color(0xFF4CAF50)
                                Difficulty.MODERATE -> Color(0xFFFFA726)
                                Difficulty.HARD -> Color(0xFFEF5350)
                            },
                            RoundedCornerShape(2.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
}

/**
 * Format time in MM:SS or HH:MM:SS format.
 */
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
