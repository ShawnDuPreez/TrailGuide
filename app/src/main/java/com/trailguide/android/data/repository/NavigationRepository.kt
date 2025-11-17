package com.trailguide.android.data.repository

import android.location.Location
import com.trailguide.android.data.local.NavigationSessionDao
import com.trailguide.android.data.local.NavigationSessionEntity
import com.trailguide.android.data.local.NavigationWaypointDao
import com.trailguide.android.data.local.NavigationWaypointEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationRepository @Inject constructor(
    private val sessionDao: NavigationSessionDao,
    private val waypointDao: NavigationWaypointDao
) {

    suspend fun startSession(
        trailId: String,
        trailName: String,
        totalDistanceMeters: Double
    ): NavigationSessionEntity {
        val session = NavigationSessionEntity(
            id = UUID.randomUUID().toString(),
            trailId = trailId,
            trailName = trailName,
            startedAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis(),
            totalDistanceMeters = totalDistanceMeters,
            currentState = "ACTIVE"
        )
        sessionDao.insertSession(session)
        return session
    }

    suspend fun updateSession(
        sessionId: String,
        distanceTraveledMeters: Double,
        durationMillis: Long,
        pausedDurationMillis: Long = 0L,
        averagePaceMinPerKm: Double? = null,
        elevationGainMeters: Int? = null,
        state: String = "ACTIVE"
    ) {
        val existing = sessionDao.getSession(sessionId) ?: return
        val updated = existing.copy(
            distanceTraveledMeters = distanceTraveledMeters,
            durationMillis = durationMillis,
            pausedDurationMillis = pausedDurationMillis,
            averagePaceMinPerKm = averagePaceMinPerKm ?: existing.averagePaceMinPerKm,
            elevationGainMeters = elevationGainMeters ?: existing.elevationGainMeters,
            currentState = state,
            updatedAtMillis = System.currentTimeMillis()
        )
        sessionDao.updateSession(updated)
    }

    suspend fun completeSession(
        sessionId: String,
        stats: NavigationSessionEntity
    ) {
        val existing = sessionDao.getSession(sessionId) ?: return
        sessionDao.updateSession(
            existing.copy(
                distanceTraveledMeters = stats.distanceTraveledMeters,
                durationMillis = stats.durationMillis,
                pausedDurationMillis = stats.pausedDurationMillis,
                averagePaceMinPerKm = stats.averagePaceMinPerKm,
                elevationGainMeters = stats.elevationGainMeters,
                completedAtMillis = System.currentTimeMillis(),
                currentState = "COMPLETED",
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordWaypoint(sessionId: String, location: Location) {
        val waypoint = NavigationWaypointEntity(
            sessionId = sessionId,
            timestampMillis = System.currentTimeMillis(),
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = location.altitude.takeIf { it != 0.0 },
            accuracyMeters = location.accuracy,
            speedMetersPerSecond = location.speed.takeIf { it != 0f },
            bearingDegrees = location.bearing.takeIf { it != 0f }
        )
        waypointDao.insertWaypoint(waypoint)
    }

    suspend fun getWaypoints(sessionId: String): List<NavigationWaypointEntity> {
        return waypointDao.getWaypointsForSession(sessionId)
    }

    suspend fun clearWaypoints(sessionId: String) {
        waypointDao.deleteWaypointsForSession(sessionId)
    }

    fun getActiveSessions(): Flow<List<NavigationSessionEntity>> {
        return sessionDao.getSessionsByState("ACTIVE")
    }

    suspend fun getSession(sessionId: String): NavigationSessionEntity? {
        return sessionDao.getSession(sessionId)
    }
}

