package com.trailguide.android.data.local

/**
 * Sync status for offline-first entities.
 * Tracks whether local changes need to be synced to remote server.
 */
enum class SyncStatus {
    /** Entity is synced with remote server */
    SYNCED,
    
    /** Entity has local changes pending sync */
    PENDING,
    
    /** Entity sync failed and needs retry */
    FAILED,
    
    /** Entity is currently being synced */
    SYNCING
}

