package com.trailguide.android.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for managing sync operations using WorkManager.
 * Handles periodic sync, one-off sync, and connectivity-based sync.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "SyncScheduler"
        private const val PERIODIC_SYNC_WORK_NAME = "periodic_sync"
        private const val ONE_TIME_SYNC_WORK_NAME = "one_time_sync"
        private const val SYNC_INTERVAL_HOURS = 6L
    }
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedule periodic sync every 6 hours.
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(PERIODIC_SYNC_WORK_NAME)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
        
        Log.d(TAG, "Scheduled periodic sync every $SYNC_INTERVAL_HOURS hours")
    }
    
    /**
     * Schedule one-time sync immediately.
     */
    fun scheduleOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(ONE_TIME_SYNC_WORK_NAME)
            .build()
        
        workManager.enqueueUniqueWork(
            ONE_TIME_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
        
        Log.d(TAG, "Scheduled one-time sync")
    }
    
    /**
     * Cancel all scheduled sync work.
     */
    fun cancelAllSync() {
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(ONE_TIME_SYNC_WORK_NAME)
        Log.d(TAG, "Cancelled all sync work")
    }
    
    /**
     * Cancel periodic sync only.
     */
    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        Log.d(TAG, "Cancelled periodic sync")
    }
    
    /**
     * Get sync work info to observe sync status.
     */
    fun getSyncWorkInfo() = workManager.getWorkInfosForUniqueWorkLiveData(ONE_TIME_SYNC_WORK_NAME)
    
    /**
     * Check if sync is currently running.
     */
    suspend fun isSyncRunning(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(ONE_TIME_SYNC_WORK_NAME).await()
        return workInfos.any { it.state == WorkInfo.State.RUNNING }
    }
}

