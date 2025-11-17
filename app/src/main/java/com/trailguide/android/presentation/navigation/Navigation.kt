package com.trailguide.android.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.trailguide.android.R
import com.trailguide.android.data.model.RoutePoint
import com.trailguide.android.presentation.screens.*
import com.trailguide.android.presentation.screens.navigation.NavigationModeScreen
import com.trailguide.android.presentation.viewmodel.TrailDetailsViewModel
import com.trailguide.android.services.NavigationServiceManager

/**
 * Main navigation destinations for the app.
 */
sealed class Screen(val route: String, val title: String) {
    object Trails : Screen("trails", "All Trails")
    object MyTrails : Screen("my_trails", "My Trails")
    object TrailDetails : Screen("trail/{trailId}", "Trail Details") {
        fun createRoute(trailId: String) = "trail/$trailId"
    }
    object Hiking : Screen("hiking/{trailId}", "Hiking") {
        fun createRoute(trailId: String) = "hiking/$trailId"
    }
    object NavigationMode : Screen("navigation/{trailId}/{trailName}", "Navigation") {
        fun createRoute(trailId: String, trailName: String) = "navigation/$trailId/${Uri.encode(trailName)}"
    }
    object Map : Screen("map", "Map")
    object Favorites : Screen("favorites", "Favorites")
    object Downloads : Screen("downloads", "Downloads")
    object Profile : Screen("profile", "Profile")
}

/**
 * Main app composable with bottom navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailGuideApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    Scaffold(
        topBar = {
            // Top bar with app title
            TopAppBar(
                title = { Text("TrailGuide") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                bottomNavItems.forEach { item ->
                    val itemTitle = stringResource(item.titleResId)
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = itemTitle) },
                        label = { Text(itemTitle) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                // Pop up to the start destination
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Trails.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Trails.route) {
                TrailsScreen(
                    onTrailClick = { trailId ->
                        navController.navigate(Screen.TrailDetails.createRoute(trailId))
                    }
                )
            }
            
            composable(Screen.MyTrails.route) {
                MyTrailsScreen(
                    onTrailClick = { trailId ->
                        navController.navigate(Screen.TrailDetails.createRoute(trailId))
                    }
                )
            }
            
            composable(
                route = Screen.TrailDetails.route,
                arguments = listOf(navArgument("trailId") { type = NavType.StringType })
            ) { backStackEntry ->
                val trailId = backStackEntry.arguments?.getString("trailId") ?: ""
                val context = LocalContext.current
                TrailDetailsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onStartHike = { trail ->
                        val routePolyline = ArrayList(trail.routeCoordinates.toLatLngList())
                        NavigationServiceManager.startNavigation(
                            context = context,
                            trailId = trail.id,
                            trailName = trail.name,
                            totalDistanceMeters = (trail.distanceKm ?: 0.0) * 1000,
                            routePolyline = if (routePolyline.isEmpty()) null else routePolyline
                        )
                        navController.navigate(Screen.NavigationMode.createRoute(trail.id, trail.name))
                    }
                )
            }
            
            composable(
                route = Screen.Hiking.route,
                arguments = listOf(navArgument("trailId") { type = NavType.StringType })
            ) { backStackEntry ->
                val trailId = backStackEntry.arguments?.getString("trailId") ?: ""
                HikingScreenWithTrail(
                    trailId = trailId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.NavigationMode.route,
                arguments = listOf(
                    navArgument("trailId") { type = NavType.StringType },
                    navArgument("trailName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val trailId = backStackEntry.arguments?.getString("trailId") ?: ""
                val trailName = backStackEntry.arguments?.getString("trailName")?.let { Uri.decode(it) } ?: "Trail"
                val context = LocalContext.current
                NavigationModeScreen(
                    trailId = trailId,
                    trailName = trailName,
                    onBack = {
                        NavigationServiceManager.stopNavigation(context)
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.Map.route) {
                MapScreen()
            }
            
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onTrailClick = { trailId ->
                        navController.navigate(Screen.TrailDetails.createRoute(trailId))
                    }
                )
            }
            
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onAreaClick = { areaId ->
                        // Navigate to area details or map view
                        // For now, just navigate to map
                        navController.navigate(Screen.Map.route)
                    }
                )
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}

/**
 * Bottom navigation items.
 */
val bottomNavItems = listOf(
    BottomNavItem(
        Screen.Trails,
        R.string.trails_screen_title,
        Icons.Filled.Hiking
    ),
    BottomNavItem(
        Screen.MyTrails,
        R.string.trails_screen_title, // Reusing for now
        Icons.Filled.AccountCircle
    ),
    BottomNavItem(
        Screen.Map,
        R.string.map_screen_title,
        Icons.Filled.Map
    ),
    BottomNavItem(
        Screen.Favorites,
        R.string.favorite,
        Icons.Filled.Favorite
    ),
    BottomNavItem(
        Screen.Profile,
        R.string.profile_screen_title,
        Icons.Filled.Person
    )
)

/**
 * Data class for bottom navigation items.
 */
data class BottomNavItem(
    val screen: Screen,
    val titleResId: Int, // Changed to resource ID
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun List<RoutePoint>.toLatLngList(): List<LatLng> {
    return map { LatLng(it.latitude, it.longitude) }
}

/**
 * Hiking screen that fetches real trail data from the API.
 */
@Composable
fun HikingScreenWithTrail(
    trailId: String,
    onNavigateBack: () -> Unit,
    viewModel: TrailDetailsViewModel = hiltViewModel()
) {
    val trail by viewModel.trail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Load trail data when component is created
    LaunchedEffect(trailId) {
        if (trailId.isNotEmpty()) {
            viewModel.loadTrail(trailId)
        }
    }
    
    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        trail != null -> {
            HikingScreen(
                trail = trail!!,
                onNavigateBack = onNavigateBack
            )
        }
        errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Failed to load trail",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { viewModel.loadTrail(trailId) }) {
                        Text("Retry")
                    }
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No trail data available")
            }
        }
    }
}

