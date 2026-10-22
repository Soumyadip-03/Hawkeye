package com.soumyadip.haykeye.security

import android.content.Context
import android.database.sqlite.SQLiteDatabase as AndroidSQLiteDatabase
import android.util.Log
import net.zetetic.database.sqlcipher.SQLiteDatabase as SqlCipherDatabase

object DatabaseMigrator {

    private const val TAG = "HaykeyeDatabaseMigrator"

    init {
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load sqlcipher native library: ${e.message}")
        }
    }

    fun isDatabasePlaintext(context: Context, dbName: String): Boolean {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            return false
        }

        var db: AndroidSQLiteDatabase? = null
        return try {
            db = AndroidSQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                AndroidSQLiteDatabase.OPEN_READONLY,
            )
            val cursor = db.rawQuery("PRAGMA user_version;", null)
            val isPlaintext = cursor.moveToFirst()
            cursor.close()
            isPlaintext
        } catch (_: Exception) {
            false
        } finally {
            db?.close()
        }
    }

    fun migratePlaintextToEncrypted(context: Context, dbName: String, passphrase: ByteArray): Boolean {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists() || !isDatabasePlaintext(context, dbName)) {
            return false
        }

        // Flush any active SQLite WAL log entries into the main database file first
        try {
            val walCheck = AndroidSQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                AndroidSQLiteDatabase.OPEN_READWRITE,
            )
            val cursor = walCheck.rawQuery("PRAGMA wal_checkpoint(FULL);", null)
            cursor.moveToFirst()
            cursor.close()
            walCheck.close()
        } catch (_: Exception) {
            // Ignore if WAL checkpoint is not required
        }

        val tempDbName = "${dbName}_encrypted_temp"
        val tempDbFile = context.getDatabasePath(tempDbName)

        if (tempDbFile.exists()) {
            context.deleteDatabase(tempDbName)
        }

        Log.d(TAG, "Starting plaintext to SQLCipher database migration for '$dbName'...")

        try {
            // 1. Open a new empty encrypted SQLCipher database as main
            val encryptedDb = SqlCipherDatabase.openDatabase(
                tempDbFile.absolutePath,
                passphrase,
                null,
                SqlCipherDatabase.OPEN_READWRITE,
                null,
            )

            // 2. Attach the plaintext database as 'plaintext' with empty password KEY ''
            val plaintextPathEscaped = dbFile.absolutePath.replace("'", "''")
            encryptedDb.execSQL("ATTACH DATABASE '$plaintextPathEscaped' AS plaintext KEY '';")

            // 3. Export all schema and data from plaintext database to main encrypted database
            encryptedDb.execSQL("SELECT sqlcipher_export('main', 'plaintext');")

            // 4. Read user_version from plaintext database and set it on main
            var userVersion = 2
            try {
                val cursor = encryptedDb.rawQuery("PRAGMA plaintext.user_version;", null)
                if (cursor.moveToFirst()) {
                    userVersion = cursor.getInt(0)
                }
                cursor.close()
            } catch (_: Exception) {
                // Default to version 2 if query fails
            }

            encryptedDb.execSQL("PRAGMA user_version = $userVersion;")

            // 5. Detach plaintext database
            encryptedDb.execSQL("DETACH DATABASE plaintext;")

            // 6. Verify encrypted database table and record count
            val countCursor = encryptedDb.rawQuery("SELECT COUNT(*) FROM urgent_messages;", null)
            val isValid = countCursor.moveToFirst()
            val rowCount = if (isValid) countCursor.getInt(0) else -1
            countCursor.close()
            encryptedDb.close()

            if (rowCount < 0) {
                Log.e(TAG, "Migration verification failed for '$dbName'. Keeping plaintext database.")
                context.deleteDatabase(tempDbName)
                return false
            }

            Log.d(TAG, "Migration verified successfully ($rowCount rows exported). Swapping database files...")

            // 7. Safely delete plaintext database and swap files
            context.deleteDatabase(dbName)
            val renamed = tempDbFile.renameTo(dbFile)
            if (!renamed) {
                Log.e(TAG, "Failed to rename encrypted temp database file to '$dbName'.")
                return false
            }

            context.deleteDatabase(tempDbName)

            Log.d(TAG, "Plaintext database '$dbName' successfully migrated to SQLCipher encrypted database.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Database migration failed: ${e.message}", e)
            context.deleteDatabase(tempDbName)
            return false
        }
    }
}
