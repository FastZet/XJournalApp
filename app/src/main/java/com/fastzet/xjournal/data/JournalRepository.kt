package com.fastzet.xjournal.data

import android.content.Context
import com.fastzet.xjournal.security.JournalEncryption
import com.fastzet.xjournal.security.JournalEntry
import com.fastzet.xjournal.security.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class JournalRepository(
    context: Context,
    private val database: JournalDatabase = JournalDatabase.getDatabase(context),
    private val encryption: JournalEncryption = JournalEncryption(context)
) {
    private val journalDao = database.journalDao()

    // Get all entries by journal ID as a Flow, decrypting them as they're observed
    fun getAllEntriesByJournal(journalId: String): Flow<List<JournalEntry>> {
        return journalDao.getAllEntriesByJournal(journalId).map { entries ->
            entries.map { encryptedEntry ->
                encryption.decryptEntry(
                    com.fastzet.xjournal.security.EncryptedJournalEntry(
                        id = encryptedEntry.id,
                        journalId = encryptedEntry.journalId,
                        encryptedData = encryptedEntry.encryptedData,
                        timestamp = encryptedEntry.timestamp,
                        lastModified = encryptedEntry.lastModified
                    )
                )
            }
        }
    }

    // Save a new journal entry
    suspend fun saveEntry(entry: JournalEntry) {
        val encryptedEntry = encryption.encryptEntry(entry)
        journalDao.insertEntry(
            EncryptedJournalEntryEntity(
                id = encryptedEntry.id,
                journalId = encryptedEntry.journalId,
                encryptedData = encryptedEntry.encryptedData,
                timestamp = encryptedEntry.timestamp,
                lastModified = Instant.now().epochSecond,
                syncStatus = SyncStatus.NOT_SYNCED
            )
        )
    }

    // Delete an entry
    suspend fun deleteEntry(entry: JournalEntry) {
        val encryptedEntry = encryption.encryptEntry(entry)
        journalDao.deleteEntry(
            EncryptedJournalEntryEntity(
                id = encryptedEntry.id,
                journalId = encryptedEntry.journalId,
                encryptedData = encryptedEntry.encryptedData,
                timestamp = encryptedEntry.timestamp,
                lastModified = entry.lastModified,
                syncStatus = entry.syncStatus
            )
        )
    }

    // Get entries that need to be synced by journal ID
    suspend fun getUnsyncedEntriesByJournal(journalId: String): List<JournalEntry> {
        return journalDao.getEntriesBySyncStatus(SyncStatus.NOT_SYNCED, journalId).map { encryptedEntry ->
            encryption.decryptEntry(
                com.fastzet.xjournal.security.EncryptedJournalEntry(
                    id = encryptedEntry.id,
                    journalId = encryptedEntry.journalId,
                    encryptedData = encryptedEntry.encryptedData,
                    timestamp = encryptedEntry.timestamp,
                    lastModified = encryptedEntry.lastModified
                )
            )
        }
    }

    // Update sync status by journal ID
    suspend fun updateSyncStatus(entryId: String, status: SyncStatus, journalId: String) {
        journalDao.updateSyncStatus(entryId, status, journalId)
    }
}
