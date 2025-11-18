package com.trailguide.android.data.notification

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages scheduling and cancellation of daily weather notifications.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "NotificationScheduler"
        private const val WORK_NAME = "daily_weather_notification"
    }
    
    /**
     * Schedule daily weather notifications at specified time.
     * @param hour Hour of day (0-23), defaults to 6
     * @param minute Minute of hour (0-59), defaults to 0
     */
    fun scheduleDailyNotifications(hour: Int = 6, minute: Int = 0) {
        val workManager = WorkManager.getInstance(context)
        
        // Calculate initial delay until next occurrence of specified time
        val now = Calendar.getInstance()
        val scheduledTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // If scheduled time has passed today, set for tomorrow
        if (scheduledTime.before(now) || scheduledTime.timeInMillis == now.timeInMillis) {
            scheduledTime.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val delay = scheduledTime.timeInMillis - now.timeInMillis
        
        // Create constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create periodic work request (runs daily)
        val periodicWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.MINUTES
            )
            .build()
        
        // Enqueue unique periodic work
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
    }
    
    /**
     * Cancel scheduled daily weather notifications.
     */
    fun cancelDailyNotifications() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_NAME)
    }
    
    /**
     * Test notification immediately (for testing purposes).
     * Creates a one-time work request that runs immediately.
     */
    fun testNotificationNow() {
        Log.d(TAG, "Test notification requested - enqueueing immediate work request")
        val workManager = WorkManager.getInstance(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create one-time work request that runs immediately
        val testWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(constraints)
            .addTag("test_notification")
            .build()
        
        workManager.enqueue(testWorkRequest)
        Log.d(TAG, "Test notification work request enqueued with ID: ${testWorkRequest.id}")
        Log.d(TAG, "Check logcat with tag 'NotificationWorker' to see progress")
    }
}

