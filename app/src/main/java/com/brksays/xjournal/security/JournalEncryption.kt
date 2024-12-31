package com.brksays.xjournal.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import java.io.File
import javax.crypto.SecretKey
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

class JournalEncryption(private val context: Context) {
    companion object {
        private const val TAG = "JournalEncryption"
        private const val ENCRYPTION_KEY_SIZE = 256
        private const val GCM_NONCE_LENGTH = 12
        private const val ENCRYPTED_PREFS_FILE = "secure_journal_prefs"
        private const val KEY_USER_KEY_ENCRYPTED = "user_key_encrypted"
    }

    private val encryptionManager = EncryptionManager()
    private val gson = Gson()
    
    // Create encrypted preferences for storing sensitive data
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Encrypt a journal entry
    fun encryptEntry(entry: JournalEntry): EncryptedJournalEntry {
        val jsonEntry = gson.toJson(entry)
        val encryptedData = encryptionManager.encrypt(jsonEntry)
        
        return EncryptedJournalEntry(
            id = entry.id,
            encryptedData = Base64.encodeToString(encryptedData, Base64.NO_WRAP),
            timestamp = entry.timestamp,
            lastModified = entry.lastModified
        )
    }

    // Decrypt a journal entry
    fun decryptEntry(encryptedEntry: EncryptedJournalEntry): JournalEntry {
        val encryptedData = Base64.decode(encryptedEntry.encryptedData, Base64.NO_WRAP)
        val decryptedJson = encryptionManager.decrypt(encryptedData)
        return gson.fromJson(decryptedJson, JournalEntry::class.java)
    }

    // Prepare entry for sync (additional encryption layer for cloud storage)
    fun prepareForSync(encryptedEntry: EncryptedJournalEntry): String {
        // Add additional encryption/headers for cloud storage
        val syncData = SyncData(
            version = 1,
            timestamp = System.currentTimeMillis(),
            encryptedContent = encryptedEntry.encryptedData
        )
        return gson.toJson(syncData)
    }

    // Verify data integrity
    fun verifyDataIntegrity(entry: JournalEntry, encryptedEntry: EncryptedJournalEntry): Boolean {
        try {
            val decryptedEntry = decryptEntry(encryptedEntry)
            return entry.id == decryptedEntry.id &&
                   entry.timestamp == decryptedEntry.timestamp
        } catch (e: Exception) {
            Log.e(TAG, "Data integrity check failed: ${e.message}")
            return false
        }
    }
}

data class EncryptedJournalEntry(
    val id: String,
    val encryptedData: String,
    val timestamp: Long,
    val lastModified: Long
)

data class SyncData(
    val version: Int,
    val timestamp: Long,
    val encryptedContent: String
)
