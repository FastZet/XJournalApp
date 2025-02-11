package com.fastzet.xjournal.data

import android.content.Context
import com.fastzet.xjournal.security.JournalEncryption
import com.fastzet.xjournal.security.JournalEntry
import com.fastzet.xjournal.security.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class JournalRepository(
    context: Context,
    private val database: JournalDatabase = JournalDatabase.getDatabase(context),
    private val encryption: JournalEncryption = JournalEncryption(context)
) {
    private val journalDao = database.journalDao()

    // Get all entries as a Flow, decrypting them as they're observed
    fun getAllEntries(): Flow<List<JournalEntry>> {
        return journalDao.getAllEntries().map { entries ->
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
        syncEntry(entry)
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

    // Get entries that need to be synced
    suspend fun getUnsyncedEntries(): List<JournalEntry> {
        return journalDao.getEntriesBySyncStatus(SyncStatus.NOT_SYNCED).map { encryptedEntry ->
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

    // Update sync status
    suspend fun updateSyncStatus(entryId: String, status: SyncStatus) {
        journalDao.updateSyncStatus(entryId, status)
    }

    // Export journal entries to a file
    suspend fun exportEntries(filePath: String): Boolean {
        return try {
            val entries = getAllEntries().first()
            val file = File(filePath)
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos).use { writer ->
                    entries.forEach { entry ->
                        val encryptedEntry = encryption.encryptEntry(entry)
                        writer.write(encryptedEntry.encryptedData + "\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
