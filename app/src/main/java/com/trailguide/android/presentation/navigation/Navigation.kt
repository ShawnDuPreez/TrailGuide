package com.trailguide.android.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.trailguide.android.presentation.screens.*
import com.trailguide.android.presentation.viewmodel.TrailDetailsViewModel

/**
 * Main navigation destinations for the app.
 */
sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
    object Trails : Screen("trails", "Trails")
    object TrailDetails : Screen("trail/{trailId}", "Trail Details") {
        fun createRoute(trailId: String) = "trail/$trailId"
    }
    object Hiking : Screen("hiking/{trailId}", "Hiking") {
        fun createRoute(trailId: String) = "hiking/$trailId"
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
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onLoginSuccess = { navController.navigate(Screen.Trails.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }}
                )
            }
            
            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = { navController.navigate(Screen.Trails.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }}
                )
            }
            
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
            ) { backStackEntry ->
                val trailId = backStackEntry.arguments?.getString("trailId") ?: ""
                TrailDetailsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onStartHike = { navController.navigate(Screen.Hiking.createRoute(trailId)) }
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
                    onTrailClick = { trailId ->
                        navController.navigate(Screen.TrailDetails.createRoute(trailId))
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
        "Trails",
        Icons.Filled.Hiking
    ),
    BottomNavItem(
        Screen.Favorites,
        "Favorites",
        Icons.Filled.Favorite
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

