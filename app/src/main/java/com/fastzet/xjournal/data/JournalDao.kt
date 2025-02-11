package com.fastzet.xjournal.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM encrypted_entries WHERE journalId = :journalId ORDER BY timestamp DESC")
    fun getAllEntriesByJournal(journalId: String): Flow<List<EncryptedJournalEntryEntity>>

    @Query("SELECT * FROM encrypted_entries WHERE id = :id AND journalId = :journalId")
    suspend fun getEntry(id: String, journalId: String): EncryptedJournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EncryptedJournalEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: EncryptedJournalEntryEntity)

    @Query("SELECT * FROM encrypted_entries WHERE syncStatus = :status AND journalId = :journalId")
    suspend fun getEntriesBySyncStatus(status: SyncStatus, journalId: String): List<EncryptedJournalEntryEntity>

    @Query("UPDATE encrypted_entries SET syncStatus = :newStatus WHERE id = :id AND journalId = :journalId")
    suspend fun updateSyncStatus(id: String, newStatus: SyncStatus, journalId: String)
}
