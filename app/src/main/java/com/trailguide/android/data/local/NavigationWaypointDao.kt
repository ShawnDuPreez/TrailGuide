package com.trailguide.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NavigationWaypointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: NavigationWaypointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoints(waypoints: List<NavigationWaypointEntity>)

    @Query("SELECT * FROM navigation_waypoints WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    suspend fun getWaypointsForSession(sessionId: String): List<NavigationWaypointEntity>

    @Query("DELETE FROM navigation_waypoints WHERE sessionId = :sessionId")
    suspend fun deleteWaypointsForSession(sessionId: String)
}

