package com.brksays.xjournal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.brksays.xjournal.security.SyncStatus

@Entity(tableName = "encrypted_entries")
data class EncryptedJournalEntryEntity(
    @PrimaryKey
    val id: String,
    val encryptedData: String,
    val timestamp: Long,
    val lastModified: Long,
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED
)

// Type converter for Room
class Converters {
    @TypeConverter
    fun toSyncStatus(value: String) = enumValueOf<SyncStatus>(value)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus) = value.name
}
