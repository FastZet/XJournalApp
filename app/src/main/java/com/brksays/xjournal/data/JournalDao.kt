package com.brksays.xjournal.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM encrypted_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<EncryptedJournalEntryEntity>>

    @Query("SELECT * FROM encrypted_entries WHERE id = :id")
    suspend fun getEntry(id: String): EncryptedJournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EncryptedJournalEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: EncryptedJournalEntryEntity)

    @Query("SELECT * FROM encrypted_entries WHERE syncStatus = :status")
    suspend fun getEntriesBySyncStatus(status: SyncStatus): List<EncryptedJournalEntryEntity>

    @Query("UPDATE encrypted_entries SET syncStatus = :newStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, newStatus: SyncStatus)
}
