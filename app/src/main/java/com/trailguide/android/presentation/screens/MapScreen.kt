package com.trailguide.android.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.trailguide.android.presentation.theme.Primary

/**
 * Map screen displaying trail locations using Google Maps.
 * Shows trails as markers and allows navigation.
 */
@Composable
fun MapScreen() {
    // Default location: Magaliesberg, South Africa
    val defaultLocation = LatLng(-25.792, 27.946)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }
    
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = mapType),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true,
                myLocationButtonEnabled = true
            )
        ) {
            // Add trail markers
            Marker(
                state = MarkerState(position = LatLng(-25.792, 27.946)),
                title = "Mount Lion Ridge",
                snippet = "Moderate • 8.4 km"
            )
            
            Marker(
                state = MarkerState(position = LatLng(-25.886, 27.983)),
                title = "Cedar Valley Loop",
                snippet = "Easy • 4.2 km"
            )
            
            Marker(
                state = MarkerState(position = LatLng(-29.0, 29.35)),
                title = "Drakensberg Peak Ascent",
                snippet = "Hard • 14.7 km"
            )
        }
        
        // Map type selector
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
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
        
        // Info card at bottom
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
                    "Trail Map",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Tap on markers to view trail information",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

