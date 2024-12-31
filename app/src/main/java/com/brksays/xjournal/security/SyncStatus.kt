package com.brksays.xjournal.security

enum class SyncStatus {
    SYNCED,        // Entry is synchronized with cloud storage
    NOT_SYNCED,    // Entry needs to be synchronized
    PENDING_SYNC,  // Sync is in progress
    SYNC_ERROR     // Error occurred during synchronization
}
