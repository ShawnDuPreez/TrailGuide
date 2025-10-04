package com.trailguide.android.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trailguide.android.presentation.screens.*

/**
 * Main navigation destinations for the app.
 */
sealed class Screen(val route: String, val title: String) {
    object Trails : Screen("trails", "Trails")
    object TrailDetails : Screen("trail/{trailId}", "Trail Details") {
        fun createRoute(trailId: String) = "trail/$trailId"
    }
    object Map : Screen("map", "Map")
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
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
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
            
            composable(
                route = Screen.TrailDetails.route,
                arguments = listOf(navArgument("trailId") { type = NavType.StringType })
            ) {
                TrailDetailsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) }
                )
            }
            
            composable(Screen.Map.route) {
                MapScreen()
            }
            
            composable(Screen.Downloads.route) {
                DownloadsScreen()
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
        "Trails",
        Icons.Filled.Hiking
    ),
    BottomNavItem(
        Screen.Map,
        "Map",
        Icons.Filled.Map
    ),
    BottomNavItem(
        Screen.Downloads,
        "Downloads",
        Icons.Filled.Download
    ),
    BottomNavItem(
        Screen.Profile,
        "Profile",
        Icons.Filled.Person
    )
)

/**
 * Data class for bottom navigation items.
 */
data class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

