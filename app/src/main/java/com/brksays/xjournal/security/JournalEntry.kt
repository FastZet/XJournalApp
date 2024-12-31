package com.brksays.xjournal.security

import java.time.Instant
import java.util.UUID

data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = Instant.now().epochSecond,
    val title: String,
    val content: String,
    val lastModified: Long = Instant.now().epochSecond,
    var syncStatus: SyncStatus = SyncStatus.NOT_SYNCED
)

enum class SyncStatus {
    NOT_SYNCED,    // Default state, never synced
    SYNCED,        // Successfully synced to Drive
    PENDING_SYNC,  // Changes made locally, needs sync
    SYNC_ERROR     // Error occurred during last sync
}
