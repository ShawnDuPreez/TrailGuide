package com.trailguide.android.services

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.trailguide.android.data.model.NavigationState
import com.trailguide.android.data.model.NavigationStats
import com.trailguide.android.data.repository.NavigationRepository
import com.trailguide.android.data.repository.NavigationStateHolder
import com.trailguide.android.presentation.MainActivity
import com.trailguide.android.util.NavigationNotificationHelper
import com.trailguide.android.util.NotificationUtil
import com.trailguide.android.util.PolylineUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class NavigationService : Service() {

    @Inject
    lateinit var navigationRepository: NavigationRepository

    @Inject
    lateinit var navigationStateHolder: NavigationStateHolder

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val notificationManager by lazy {
        NotificationManagerCompat.from(this)
    }

    private var locationRequest: LocationRequest? = null
    private var lastLocation: Location? = null
    private var isPaused = false
    private var pauseStartedAt: Long = 0L
    private var pausedDurationMillis: Long = 0L

    private var currentSessionId: String? = null
    private var currentTrailId: String? = null
    private var currentTrailName: String? = null
    private var totalDistanceMeters: Double = 0.0
    private var routePolyline: ArrayList<LatLng>? = null
    private var navigationStartedAtMillis: Long = 0L
    private var manualDistanceMeters: Double = 0.0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationUtil.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_PAUSE -> pauseNavigation()
            ACTION_RESUME -> resumeNavigation()
            ACTION_STOP -> stopNavigation()
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }
        return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        if (currentSessionId != null) {
            Log.d(TAG, "Navigation already running")
            return
        }
        val trailId = intent.getStringExtra(EXTRA_TRAIL_ID) ?: return
        val trailName = intent.getStringExtra(EXTRA_TRAIL_NAME) ?: "Trail"
        val totalDistance = intent.getDoubleExtra(EXTRA_TOTAL_DISTANCE_METERS, 0.0)
        val polyline = intent.getParcelableArrayListExtra<LatLng>(EXTRA_ROUTE_POLYLINE)

        currentTrailId = trailId
        currentTrailName = trailName
        totalDistanceMeters = if (polyline != null && polyline.size > 1) {
            PolylineUtils.calculateTotalDistance(polyline)
        } else {
            totalDistance
        }
        routePolyline = polyline
        navigationStartedAtMillis = System.currentTimeMillis()
        pausedDurationMillis = 0L
        isPaused = false
        manualDistanceMeters = 0.0

        serviceScope.launch {
            val session = navigationRepository.startSession(
                trailId = trailId,
                trailName = trailName,
                totalDistanceMeters = totalDistanceMeters
            )
            currentSessionId = session.id
            navigationStateHolder.update(NavigationState.Preparing(trailId, trailName))
            startForegroundService()
            startLocationUpdates()
        }
    }

    private fun startForegroundService() {
        val notification = NavigationNotificationHelper.buildNavigationNotification(
            context = this,
            stats = null,
            isPaused = false,
            contentIntent = buildContentIntent(),
            pauseIntent = buildServicePendingIntent(ACTION_PAUSE),
            resumeIntent = buildServicePendingIntent(ACTION_RESUME),
            stopIntent = buildServicePendingIntent(ACTION_STOP)
        )
        startForeground(NotificationUtil.NOTIFICATION_ID_NAVIGATION, notification)
    }

    private fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TimeUnit.SECONDS.toMillis(2)
        )
            .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(1))
            .setMaxUpdateDelayMillis(TimeUnit.SECONDS.toMillis(5))
            .build()

        locationRequest?.let { request ->
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (isPaused) return
            for (location in result.locations) {
                handleLocationUpdate(location)
            }
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val sessionId = currentSessionId ?: return

        serviceScope.launch {
            navigationRepository.recordWaypoint(sessionId, location)
        }

        val elapsedMillis = System.currentTimeMillis() - navigationStartedAtMillis - pausedDurationMillis
        val distanceTraveled = calculateDistanceTraveled(location)
        val pace = PolylineUtils.calculatePaceMinPerKm(elapsedMillis, distanceTraveled)
        val distanceRemaining = (totalDistanceMeters - distanceTraveled).coerceAtLeast(0.0)
        val eta = PolylineUtils.calculateEtaMillis(System.currentTimeMillis(), distanceRemaining, pace)

        val stats = NavigationStats(
            trailId = currentTrailId.orEmpty(),
            trailName = currentTrailName.orEmpty(),
            totalDistanceMeters = totalDistanceMeters,
            distanceTraveledMeters = distanceTraveled,
            distanceRemainingMeters = distanceRemaining,
            progressPercent = if (totalDistanceMeters > 0) {
                ((distanceTraveled / totalDistanceMeters) * 100).toFloat().coerceIn(0f, 100f)
            } else {
                0f
            },
            currentPaceMinPerKm = pace,
            averagePaceMinPerKm = pace,
            elapsedMillis = elapsedMillis,
            etaMillis = eta,
            elevationGainMeters = 0,
            currentElevationMeters = location.altitude.toInt(),
            gpsAccuracyMeters = location.accuracy,
            lastKnownLatitude = location.latitude,
            lastKnownLongitude = location.longitude
        )

        navigationStateHolder.update(
            NavigationState.Active(
                sessionId = sessionId,
                stats = stats,
                isPaused = isPaused
            )
        )

        serviceScope.launch {
            navigationRepository.updateSession(
                sessionId = sessionId,
                distanceTraveledMeters = distanceTraveled,
                durationMillis = elapsedMillis,
                pausedDurationMillis = pausedDurationMillis,
                averagePaceMinPerKm = pace
            )
        }

        updateNotification(stats)
        lastLocation = location
    }

    private fun calculateDistanceTraveled(location: Location): Double {
        val polyline = routePolyline
        val userLatLng = LatLng(location.latitude, location.longitude)
        return if (polyline != null && polyline.size > 1) {
            val progress = PolylineUtils.computeProgress(userLatLng, polyline)
            progress?.distanceFromStartMeters ?: 0.0
        } else {
            // Fallback: accumulate distance via GPS points
            val previous = lastLocation
            if (previous != null) {
                manualDistanceMeters += previous.distanceTo(location)
            }
            manualDistanceMeters
        }
    }

    private fun pauseNavigation() {
        if (isPaused) return
        isPaused = true
        pauseStartedAt = System.currentTimeMillis()
        navigationStateHolder.state.value.let { state ->
            if (state is NavigationState.Active) {
                navigationStateHolder.update(state.copy(isPaused = true))
                updateNotification(state.stats)
            }
        }
    }

    private fun resumeNavigation() {
        if (!isPaused) return
        isPaused = false
        val pauseDuration = System.currentTimeMillis() - pauseStartedAt
        pausedDurationMillis += pauseDuration
        navigationStateHolder.state.value.let { state ->
            if (state is NavigationState.Active) {
                navigationStateHolder.update(state.copy(isPaused = false))
                updateNotification(state.stats)
            }
        }
    }

    private fun stopNavigation() {
        stopLocationUpdates()
        serviceScope.launch {
            currentSessionId?.let { sessionId ->
                navigationRepository.getSession(sessionId)?.let { session ->
                    navigationRepository.completeSession(sessionId, session)
                }
            }
        }
        navigationStateHolder.update(NavigationState.Idle)
        NotificationUtil.cancelNavigationNotification(this)
        currentSessionId = null
        stopSelf()
    }

    private fun updateNotification(stats: NavigationStats?) {
        val notification = NavigationNotificationHelper.buildNavigationNotification(
            context = this,
            stats = stats,
            isPaused = isPaused,
            contentIntent = buildContentIntent(),
            pauseIntent = buildServicePendingIntent(ACTION_PAUSE),
            resumeIntent = buildServicePendingIntent(ACTION_RESUME),
            stopIntent = buildServicePendingIntent(ACTION_STOP)
        )
        notificationManager.notify(NotificationUtil.NOTIFICATION_ID_NAVIGATION, notification)
    }

    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, NavigationService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "NavigationService"

        const val ACTION_START = "com.trailguide.android.navigation.ACTION_START"
        const val ACTION_PAUSE = "com.trailguide.android.navigation.ACTION_PAUSE"
        const val ACTION_RESUME = "com.trailguide.android.navigation.ACTION_RESUME"
        const val ACTION_STOP = "com.trailguide.android.navigation.ACTION_STOP"

        const val EXTRA_TRAIL_ID = "extra_trail_id"
        const val EXTRA_TRAIL_NAME = "extra_trail_name"
        const val EXTRA_TOTAL_DISTANCE_METERS = "extra_total_distance"
        const val EXTRA_ROUTE_POLYLINE = "extra_route_polyline"
    }
}

