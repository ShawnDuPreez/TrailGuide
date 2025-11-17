# 🧭 Real-Time Navigation Implementation Plan
## Google Maps Style Trip Notification + Live Tracking

---

## ✅ Current State

### Already Implemented:
- ✅ FCM push notifications (weather, new trails, friend activity)
- ✅ NotificationUtil with basic channels
- ✅ Google Maps SDK integrated
- ✅ Room Database setup
- ✅ FusedLocationProvider (basic usage)
- ✅ MVVM architecture
- ✅ Trail model with segments and route coordinates

### Missing for Navigation:
- ❌ Foreground Service for continuous tracking
- ❌ Navigation-specific notification channel
- ❌ Live trip notification with progress bar
- ❌ GPS tracking during navigation
- ❌ Progress calculation along polyline
- ❌ Navigation state management
- ❌ Navigation UI screen
- ❌ RoomDB entities for navigation history

---

## 📋 Implementation Plan

### Phase 1: Core Navigation Service (HIGH PRIORITY)
**Files to Create:**

1. **`app/src/main/java/com/trailguide/android/services/NavigationService.kt`**
   - Foreground Service
   - `startForeground()` with ongoing notification
   - Location updates via FusedLocationProvider
   - Broadcast navigation updates
   - Handle Pause/Stop actions

2. **`app/src/main/java/com/trailguide/android/data/model/NavigationState.kt`**
   - Sealed class for: Idle, Starting, Active, Paused, Stopped, Error
   - Navigation stats data class (distance, ETA, pace, etc.)

3. **`app/src/main/java/com/trailguide/android/util/NavigationNotificationHelper.kt`**
   - Create navigation notification channel
   - Build ongoing notification with:
     - Trail name
     - Progress bar
     - Distance remaining
     - ETA
     - Current pace
     - Elevation
     - Pause/Stop action buttons
   - Update notification method

4. **`app/src/main/java/com/trailguide/android/util/PolylineUtils.kt`**
   - Calculate nearest point on polyline
   - Calculate distance along polyline
   - Calculate distance remaining
   - Snap GPS to polyline
   - Calculate ETA based on pace

**Manifest Changes:**
```xml
<!-- Add to AndroidManifest.xml -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".services.NavigationService"
    android:foregroundServiceType="location"
    android:exported="false" />
```

**Gradle Dependencies:**
```kotlin
// Already have these, but verify versions:
implementation("com.google.android.gms:play-services-location:21.1.0")
implementation("com.google.android.gms:play-services-maps:18.2.0")
```

---

### Phase 2: Data Layer

5. **`app/src/main/java/com/trailguide/android/data/local/NavigationSessionEntity.kt`**
   ```kotlin
   @Entity(tableName = "navigation_sessions")
   data class NavigationSessionEntity(
       @PrimaryKey val id: String = UUID.randomUUID().toString(),
       val trailId: String,
       val trailName: String,
       val startTime: Long,
       val endTime: Long?,
       val totalDistance: Double, // meters
       val completedDistance: Double, // meters
       val duration: Long, // milliseconds
       val averagePace: Double, // min/km
       val elevationGain: Int,
       val maxElevation: Int,
       val minElevation: Int,
       val pausedDuration: Long,
       val status: String, // ACTIVE, PAUSED, COMPLETED, ABANDONED
       val gpsAccuracy: Float,
       val syncStatus: String = SyncStatus.PENDING.name
   )
   ```

6. **`app/src/main/java/com/trailguide/android/data/local/NavigationSessionDao.kt`**
   ```kotlin
   @Dao
   interface NavigationSessionDao {
       @Insert
       suspend fun insertSession(session: NavigationSessionEntity): Long
       
       @Update
       suspend fun updateSession(session: NavigationSessionEntity)
       
       @Query("SELECT * FROM navigation_sessions WHERE id = :sessionId")
       suspend fun getSession(sessionId: String): NavigationSessionEntity?
       
       @Query("SELECT * FROM navigation_sessions WHERE trailId = :trailId ORDER BY startTime DESC")
       fun getSessionsForTrail(trailId: String): Flow<List<NavigationSessionEntity>>
       
       @Query("SELECT * FROM navigation_sessions WHERE status = :status")
       fun getActiveSessions(status: String = "ACTIVE"): Flow<List<NavigationSessionEntity>>
       
       @Query("SELECT * FROM navigation_sessions ORDER BY startTime DESC LIMIT :limit")
       fun getRecentSessions(limit: Int = 10): Flow<List<NavigationSessionEntity>>
   }
   ```

7. **`app/src/main/java/com/trailguide/android/data/local/NavigationWaypointEntity.kt`**
   ```kotlin
   @Entity(tableName = "navigation_waypoints")
   data class NavigationWaypointEntity(
       @PrimaryKey(autoGenerate = true) val id: Long = 0,
       val sessionId: String,
       val timestamp: Long,
       val latitude: Double,
       val longitude: Double,
       val altitude: Double?,
       val accuracy: Float,
       val speed: Float?,
       val bearing: Float?
   )
   ```

8. **Update `TrailDatabase.kt`**:
   - Add new entities: `NavigationSessionEntity`, `NavigationWaypointEntity`
   - Add DAOs
   - Increment version to 5
   - Add migration

---

### Phase 3: Repository & ViewModel

9. **`app/src/main/java/com/trailguide/android/data/repository/NavigationRepository.kt`**
   ```kotlin
   class NavigationRepository @Inject constructor(
       private val sessionDao: NavigationSessionDao,
       private val waypointDao: NavigationWaypointDao,
       private val locationRepository: LocationRepository
   ) {
       suspend fun startNavigation(trailId: String, trailName: String): String
       suspend fun pauseNavigation(sessionId: String)
       suspend fun resumeNavigation(sessionId: String)
       suspend fun stopNavigation(sessionId: String)
       suspend fun recordWaypoint(sessionId: String, location: Location)
       fun getActiveSession(): Flow<NavigationSessionEntity?>
       suspend fun calculateStats(sessionId: String): NavigationStats
   }
   ```

10. **`app/src/main/java/com/trailguide/android/presentation/viewmodel/NavigationViewModel.kt`**
    ```kotlin
    @HiltViewModel
    class NavigationViewModel @Inject constructor(
        private val navigationRepository: NavigationRepository,
        private val trailRepository: TrailRepository
    ) : ViewModel() {
        val navigationState: StateFlow<NavigationState>
        val currentStats: StateFlow<NavigationStats?>
        val trailPolyline: StateFlow<List<LatLng>>
        val userLocation: StateFlow<LatLng?>
        
        fun startNavigation(trailId: String)
        fun pauseNavigation()
        fun resumeNavigation()
        fun stopNavigation()
    }
    ```

---

### Phase 4: UI Layer

11. **`app/src/main/java/com/trailguide/android/presentation/screens/navigation/NavigationModeScreen.kt`**
    - Full-screen Google Map
    - Top card with stats:
      - Trail name
      - Distance completed / total
      - Progress bar
      - ETA
      - Current pace
      - Elevation
    - Bottom controls:
      - Pause button
      - Stop button
      - Recenter button
    - Polyline showing trail route
    - User location dot
    - Trail markers (start/end)

12. **Add to `Navigation.kt`**:
    ```kotlin
    object NavigationMode : Screen("navigation/{trailId}", "Navigation")
    ```

13. **Update `TrailDetailsScreen.kt`**:
    - Add "Start Navigation" button
    - Check location permissions
    - Navigate to NavigationModeScreen

---

### Phase 5: Integration & Polish

14. **Location Permissions Check**
    - Request FINE_LOCATION and BACKGROUND_LOCATION
    - Request POST_NOTIFICATIONS (Android 13+)
    - Handle permission denial gracefully

15. **Battery Optimization**
    - Use `PRIORITY_HIGH_ACCURACY` during active navigation
    - Use `PRIORITY_BALANCED_POWER_ACCURACY` when paused
    - Stop updates when navigation ends

16. **Error Handling**
    - GPS signal lost
    - User went off-trail
    - Service crash recovery
    - Background location denied

17. **Sync to Supabase**
    - Create `navigation_sessions` table in Supabase
    - Sync completed sessions
    - Store waypoints for replay

---

## 🎯 Implementation Order

### Step 1: Foundation (Start Here)
1. Create `NavigationState.kt` model
2. Create `PolylineUtils.kt` for calculations
3. Create `NavigationNotificationHelper.kt`
4. Add manifest permissions

### Step 2: Service
5. Implement `NavigationService.kt`
6. Test foreground service with dummy notification

### Step 3: Data
7. Create Room entities (Session, Waypoint)
8. Create DAOs
9. Update database version
10. Create `NavigationRepository.kt`

### Step 4: ViewModel
11. Create `NavigationViewModel.kt`
12. Implement state management
13. Connect to repository

### Step 5: UI
14. Create `NavigationModeScreen.kt`
15. Add "Start Navigation" to Trail Details
16. Update navigation graph

### Step 6: Testing & Polish
17. Test service lifecycle
18. Test notification updates
19. Test battery usage
20. Add error handling

---

## 📊 Notification Layout Specs

### Google Maps Style Notification

```
┌─────────────────────────────────────┐
│ 🗺️ TRAILGUIDE NAVIGATION           │
├─────────────────────────────────────┤
│ Lion's Head Trail                   │
│ [████████░░░░] 75% complete         │
│                                     │
│ 📍 1.2 km remaining                 │
│ ⏱️ ETA: 15:45 PM                    │
│ 🏃 Pace: 12:30 /km                  │
│ 📈 Elevation: 645m                  │
│                                     │
│ [⏸ Pause]  [⏹ Stop]                │
└─────────────────────────────────────┘
```

---

## 🔧 Key Technical Components

### 1. Distance Calculation Along Polyline
```kotlin
fun calculateDistanceAlongPolyline(
    userLocation: LatLng,
    polyline: List<LatLng>,
    tolerance: Double = 50.0 // meters
): PolylineProgress {
    // Find nearest point on polyline
    // Calculate distance from start
    // Calculate distance to end
    // Return progress percentage
}
```

### 2. ETA Calculation
```kotlin
fun calculateETA(
    distanceRemaining: Double, // meters
    recentPace: Double, // min/km
    averagePace: Double // min/km
): Long {
    // Weight recent pace more heavily
    // Return timestamp
}
```

### 3. GPS Accuracy Filtering
```kotlin
fun isLocationAccurate(location: Location): Boolean {
    return location.accuracy < 20f // meters
}
```

---

## 📱 Required Permissions

```xml
<!-- Location (already have these) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Background location (for Android 10+) -->
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<!-- Foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

<!-- Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## 🎨 UI Screens

### NavigationModeScreen Layout
```
┌──────────────────────────────┐
│ ← Back                    ☰  │ <- Top bar
├──────────────────────────────┤
│                              │
│       GOOGLE MAP VIEW        │
│     (with trail polyline)    │
│     (user location dot)      │
│                              │
│         [75% done]           │
├──────────────────────────────┤
│ Lion's Head Trail            │ <- Stats card
│ Distance: 3.8 / 5.0 km       │
│ ETA: 15:45 PM                │
│ Pace: 12:30 /km              │
│ Elevation: 645m              │
├──────────────────────────────┤
│  [⏸ Pause]    [⏹ Stop]      │ <- Controls
└──────────────────────────────┘
```

---

## 🚀 Next Steps

**READY TO BEGIN?**

I will now implement this step-by-step, starting with:
1. NavigationService.kt
2. NavigationNotificationHelper.kt
3. NavigationState.kt models
4. PolylineUtils.kt

Then continue through the entire implementation.

**Shall I proceed?**

