package com.trailguide.android.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.trailguide.android.data.model.Trail
import com.trailguide.android.data.model.TrailSegment
import com.trailguide.android.presentation.theme.*
import com.trailguide.android.presentation.viewmodel.TrailDetailsViewModel

/**
 * Trail details screen showing comprehensive information about a trail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailDetailsScreen(
    viewModel: TrailDetailsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit,
    onStartHike: () -> Unit = {}
) {
    val trail by viewModel.trail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trail Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
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
                    TrailDetailsContent(
                        trail = trail!!,
                        isFavorite = isFavorite,
                        onToggleFavorite = { viewModel.toggleFavorite() },
                        onStartHike = onStartHike,
                        onDownload = { viewModel.downloadTrail() },
                        onNavigateToMap = onNavigateToMap
                    )
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = errorMessage!!, color = Error)
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrailDetailsContent(
    trail: Trail,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onStartHike: () -> Unit,
    onDownload: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero image
        AsyncImage(
            model = trail.imageUrl,
            contentDescription = trail.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentScale = ContentScale.Crop
        )
        
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with title and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = trail.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        DifficultyBadge(trail.difficulty)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = trail.city,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartHike,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Hike")
                }
                
                OutlinedButton(
                    onClick = onDownload,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download")
                }
                
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) AccentRed else TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Stats", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(Icons.Default.Terrain, "Distance", "${trail.distanceKm} km")
                        Divider(
                            modifier = Modifier
                                .height(50.dp)
                                .width(1.dp)
                        )
                        StatItem(Icons.Default.TrendingUp, "Elevation", "${trail.elevationM} m")
                        Divider(
                            modifier = Modifier
                                .height(50.dp)
                                .width(1.dp)
                        )
                        StatItem(Icons.Default.Star, "Rating", trail.rating.toString())
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Route availability card
            if (trail.routeCoordinates.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "GPS Route Available",
                                style = MaterialTheme.typography.titleSmall,
                                color = Primary
                            )
                            Text(
                                "${trail.routeCoordinates.size} GPS points • Full trail path",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Description
            trail.description?.let { desc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Segments
            if (trail.segments.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Segments", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        trail.segments.forEach { segment ->
                            SegmentItem(segment)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Reviews Section
            ReviewsSection(
                trail = trail,
                onAddReview = { /* TODO: Open add review dialog */ }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Primary)
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
fun ReviewsSection(
    trail: Trail,
    onAddReview: () -> Unit
) {
    // Mock reviews for now - in production this would come from API
    val mockReviews = listOf(
        com.trailguide.android.data.model.Review(
            id = "1",
            trailId = trail.id,
            userId = "user1",
            userName = "Sarah Johnson",
            rating = 5.0,
            comment = "Amazing trail! The views at the summit are breathtaking. Highly recommend going early morning to catch the sunrise.",
            photos = listOf("https://images.unsplash.com/photo-1551632811-561732d1e306?w=400"),
            createdAt = System.currentTimeMillis() - 86400000,
            likes = 12
        ),
        com.trailguide.android.data.model.Review(
            id = "2",
            trailId = trail.id,
            userId = "user2",
            userName = "Mike Chen",
            rating = 4.5,
            comment = "Great hike, but quite challenging in some sections. Bring plenty of water!",
            photos = listOf(),
            createdAt = System.currentTimeMillis() - 172800000,
            likes = 8
        )
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Community Reviews", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${mockReviews.size} reviews",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Button(onClick = onAddReview) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Review")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show reviews
            mockReviews.forEach { review ->
                ReviewItem(review = review)
                if (review != mockReviews.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            if (mockReviews.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No reviews yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        "Be the first to share your experience!",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: com.trailguide.android.data.model.Review) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // User info and rating
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // User avatar placeholder
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Primary.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            review.userName.first().toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        review.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(
                                if (index < review.rating.toInt()) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (index < review.rating.toInt()) Primary else TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "%.1f".format(review.rating),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            // Time ago
            Text(
                getTimeAgo(review.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Review text
        Text(
            review.comment,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Photos if available
        if (review.photos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                review.photos.take(3).forEach { photoUrl ->
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Review photo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                }
                if (review.photos.size > 3) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+${review.photos.size - 3}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Primary
                        )
                    }
                }
            }
        }
        
        // Like count
        if (review.likes > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ThumbUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${review.likes} helpful",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> "${diff / 604800000}w ago"
    }
}

@Composable
fun SegmentItem(segment: TrailSegment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                segment.name,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Surface(
            color = Secondary.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = segment.type,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Secondary
            )
        }
    }
}

