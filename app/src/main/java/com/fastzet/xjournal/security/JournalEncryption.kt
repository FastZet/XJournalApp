package com.fastzet.xjournal.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class JournalEncryption(private val context: Context) {
    companion object {
        private const val TAG = "JournalEncryption"
        private const val ENCRYPTION_KEY_SIZE = 256
        private const val GCM_NONCE_LENGTH = 12
        private const val ENCRYPTED_PREFS_FILE = "secure_journal_prefs"
        private const val KEY_USER_KEY_ENCRYPTED = "user_key_encrypted"
        private const val KEY_ALIAS = "metadata_encryption_key"
    }

    private val encryptionManager = EncryptionManager()
    private val gson = Gson()
    private val metadataEncryptionKey: SecretKey by lazy { getMetadataEncryptionKey() }

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
            journalId = entry.journalId,
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
        // Encrypt metadata (e.g., timestamp)
        val encryptedTimestamp = encryptMetadata(encryptedEntry.timestamp.toString())

        // Create SyncData with encrypted metadata
        val syncData = SyncData(
            version = 1,
            timestamp = encryptedTimestamp.toLongOrNull() ?: 0L,
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

    // Encrypt metadata (e.g., file names, timestamps)
    private fun encryptMetadata(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, metadataEncryptionKey)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    // Decrypt metadata
    private fun decryptMetadata(encryptedData: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, metadataEncryptionKey)
        val decryptedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.NO_WRAP))
        return String(decryptedBytes)
    }

    // Generate or retrieve the metadata encryption key from the Keystore
    private fun getMetadataEncryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        return if (!keyStore.containsAlias(KEY_ALIAS)) {
            // Create a new key if it doesn't exist
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(ENCRYPTION_KEY_SIZE)
                    .build()
            )
            keyGenerator.generateKey()
        } else {
            // Retrieve the existing key
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }
    }
}

data class EncryptedJournalEntry(
    val id: String,
    val journalId: String,
    val encryptedData: String,
    val timestamp: Long,
    val lastModified: Long
)

data class SyncData(
    val version: Int,
    val timestamp: Long,
    val encryptedContent: String
)
