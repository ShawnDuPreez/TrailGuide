package com.trailguide.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NavigationSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: NavigationSessionEntity)

    @Update
    suspend fun updateSession(session: NavigationSessionEntity)

    @Query("SELECT * FROM navigation_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): NavigationSessionEntity?

    @Query("SELECT * FROM navigation_sessions WHERE currentState = :state ORDER BY startedAtMillis DESC")
    fun getSessionsByState(state: String): Flow<List<NavigationSessionEntity>>

    @Query("SELECT * FROM navigation_sessions WHERE trailId = :trailId ORDER BY startedAtMillis DESC")
    fun getSessionsForTrail(trailId: String): Flow<List<NavigationSessionEntity>>

    @Query("SELECT * FROM navigation_sessions ORDER BY startedAtMillis DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 10): Flow<List<NavigationSessionEntity>>
}

