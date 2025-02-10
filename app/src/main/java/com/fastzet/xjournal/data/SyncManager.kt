package com.fastzet.xjournal.data

import android.content.Context
import android.util.Log
import com.fastzet.xjournal.security.JournalEncryption
import com.fastzet.xjournal.security.JournalEntry
import com.fastzet.xjournal.security.SyncStatus
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class SyncManager(private val context: Context) {
    companion object {
        private const val TAG = "SyncManager"
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private val encryption = JournalEncryption(context)
    private val networkManager = NetworkManager(context)
    private val googleAccountCredential = GoogleAccountCredential.usingOAuth2(
        context,
        listOf(DriveScopes.DRIVE_FILE)
    )

    init {
        // Initialize Google Drive service
        googleAccountCredential.selectedAccountName = "user@example.com" // Set the account name
    }

    private val driveService: Drive by lazy {
        Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            JacksonFactory.getDefaultInstance(),
            googleAccountCredential
        ).setApplicationName("XJournalApp").build()
    }

    suspend fun syncEntry(entry: JournalEntry) {
        if (!networkManager.isNetworkEnabled()) {
            Log.w(TAG, "Network is not enabled. Skipping sync.")
            return
        }

        val retryCount = AtomicInteger(0)
        while (retryCount.get() < MAX_RETRY_ATTEMPTS) {
            try {
                val syncData = encryption.prepareForSync(encryption.encryptEntry(entry))
                val fileMetadata = File().apply {
                    name = "${entry.timestamp}_${entry.id}.dat"
                    parents = listOf("appDataFolder") // Store in app-specific folder
                }
                val mediaContent = ByteArrayContent("application/octet-stream", syncData.toByteArray())
                val file = driveService.files().create(fileMetadata, mediaContent).execute()
                Log.d(TAG, "File ID: ${file.id}")
                updateSyncStatus(entry.id, SyncStatus.SYNCED, entry.journalId)
                break
            } catch (e: IOException) {
                retryCount.incrementAndGet()
                Log.w(TAG, "Sync failed. Attempt: ${retryCount.get()}, Error: ${e.message}")
                if (retryCount.get() >= MAX_RETRY_ATTEMPTS) {
                    updateSyncStatus(entry.id, SyncStatus.SYNC_ERROR, entry.journalId)
                    Log.e(TAG, "Max retry attempts reached. Sync failed.")
                }
            }
        }
    }

    private suspend fun updateSyncStatus(entryId: String, status: SyncStatus, journalId: String) {
        withContext(Dispatchers.IO) {
            val repository = JournalRepository(context)
            repository.updateSyncStatus(entryId, status, journalId)
        }
    }
}
