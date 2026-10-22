package com.soumyadip.haykeye.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import java.security.SecureRandom

class DatabaseKeyManager(
    context: Context,
    private val keystoreManager: KeystoreManager = KeystoreManager(),
) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun getOrCreatePassphrase(): ByteArray {
        val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivBase64 = prefs.getString(KEY_PASSPHRASE_IV, null)

        if (!encryptedBase64.isNullOrBlank() && !ivBase64.isNullOrBlank()) {
            try {
                val encryptedData = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                return keystoreManager.decryptPassphrase(encryptedData, iv)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt existing database passphrase from Keystore: ${e.message}")
                throw IllegalStateException("Database encryption passphrase cannot be decrypted. Key material may be corrupted or lost.", e)
            }
        }

        // Generate a new 256-bit (32 byte) secure random passphrase
        val newPassphrase = ByteArray(PASSPHRASE_SIZE_BYTES)
        SecureRandom().nextBytes(newPassphrase)

        try {
            val (encryptedData, iv) = keystoreManager.encryptPassphrase(newPassphrase)
            prefs.edit().apply {
                putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encryptedData, Base64.NO_WRAP))
                putString(KEY_PASSPHRASE_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                apply()
            }
            return newPassphrase
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt and store database passphrase with Keystore: ${e.message}")
            throw IllegalStateException("Failed to securely store database encryption key.", e)
        }
    }

    fun hasStoredPassphrase(): Boolean {
        return !prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null).isNullOrBlank()
    }

    companion object {
        private const val TAG = "HaykeyeDatabaseKeyManager"
        private const val PREFS_NAME = "haykeye_security_prefs"
        private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_db_passphrase"
        private const val KEY_PASSPHRASE_IV = "db_passphrase_iv"
        private const val PASSPHRASE_SIZE_BYTES = 32
    }
}
