package com.trailguide.android.presentation.screens.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.trailguide.android.data.model.NavigationState
import com.trailguide.android.presentation.viewmodel.NavigationViewModel
import com.trailguide.android.services.NavigationServiceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationModeScreen(
    trailId: String,
    trailName: String,
    routePolyline: List<LatLng> = emptyList(),
    onBack: () -> Unit,
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val navigationState by viewModel.navigationState.collectAsState()
    val context = LocalContext.current

    val lastLatLng = (navigationState as? NavigationState.Active)?.stats?.let { stats ->
        if (stats.lastKnownLatitude != null && stats.lastKnownLongitude != null) {
            LatLng(stats.lastKnownLatitude, stats.lastKnownLongitude)
        } else null
    }

    val initialLatLng = lastLatLng ?: routePolyline.firstOrNull() ?: LatLng(-25.7479, 28.2293)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 14f)
    }

    LaunchedEffect(lastLatLng) {
        if (lastLatLng != null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(lastLatLng))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navigation • $trailName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        compassEnabled = true,
                        mapToolbarEnabled = false
                    )
                ) {
                    if (routePolyline.isNotEmpty()) {
                        Polyline(
                            points = routePolyline,
                            color = Color(0xFF1DB954),
                            width = 12f
                        )
                        Marker(
                            state = MarkerState(position = routePolyline.first()),
                            title = "Trail Start"
                        )
                        Marker(
                            state = MarkerState(position = routePolyline.last()),
                            title = "Trail End"
                        )
                    }

                    lastLatLng?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "You"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NavigationStatsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = navigationState
            )

            Spacer(modifier = Modifier.height(16.dp))

            ControlButtons(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                state = navigationState,
                onPause = { NavigationServiceManager.pauseNavigation(context) },
                onResume = { NavigationServiceManager.resumeNavigation(context) },
                onStop = { NavigationServiceManager.stopNavigation(context) }
            )
        }
    }
}

@Composable
private fun NavigationStatsCard(
    modifier: Modifier = Modifier,
    state: NavigationState
) {
    val context = LocalContext.current
    val stats = (state as? NavigationState.Active)?.stats

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Text(
            text = stats?.trailName ?: "Preparing navigation...",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Progress: ${stats?.progressPercent?.toInt() ?: 0}%")
        Text("Distance: ${"%.2f".format((stats?.distanceTraveledMeters ?: 0.0) / 1000)} / ${"%.2f".format((stats?.totalDistanceMeters ?: 0.0) / 1000)} km")
        Text("Pace: ${stats?.currentPaceMinPerKm?.let { formatPaceText(it) } ?: "—"}")
        Text("ETA: ${stats?.etaMillis?.let { android.text.format.DateFormat.getTimeFormat(context).format(it) } ?: "—"}")
    }
}

@Composable
private fun ControlButtons(
    modifier: Modifier = Modifier,
    state: NavigationState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                if ((state as? NavigationState.Active)?.isPaused == true) {
                    onResume()
                } else {
                    onPause()
                }
            },
            enabled = state is NavigationState.Active
        ) {
            Text(if ((state as? NavigationState.Active)?.isPaused == true) "Resume" else "Pause")
        }
        Button(
            modifier = Modifier.weight(1f),
            onClick = onStop,
            enabled = state is NavigationState.Active
        ) {
            Text("Stop")
        }
    }
}

private fun formatPaceText(pace: Double): String {
    val minutes = pace.toInt()
    val seconds = ((pace - minutes) * 60).toInt()
    return "%d:%02d /km".format(minutes, seconds)
}

