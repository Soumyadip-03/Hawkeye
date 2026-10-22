package com.soumyadip.haykeye.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.soumyadip.haykeye.security.DatabaseKeyManager
import com.soumyadip.haykeye.security.DatabaseMigrator
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [UrgentMessage::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun urgentMessageDao(): UrgentMessageDao

    companion object {
        const val DB_NAME = "haykeye_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE urgent_messages ADD COLUMN dedupKey TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    """
                    UPDATE urgent_messages 
                    SET dedupKey = provider || '|' || COALESCE(groupName, '') || '|' || senderName || '|' || timestamp || '|' || fullText || '|legacy_' || id
                    WHERE dedupKey = '' OR dedupKey IS NULL
                    """.trimIndent(),
                )

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_urgent_messages_dedupKey ON urgent_messages(dedupKey)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext

                // 1. Get or generate encrypted 256-bit database passphrase from Keystore
                val keyManager = DatabaseKeyManager(appContext)
                val passphrase = keyManager.getOrCreatePassphrase()

                // 2. Safely migrate existing plaintext database if present
                if (DatabaseMigrator.isDatabasePlaintext(appContext, DB_NAME)) {
                    DatabaseMigrator.migratePlaintextToEncrypted(appContext, DB_NAME, passphrase)
                }

                // 3. Build Room database with SQLCipher SupportOpenHelperFactory
                val factory = SupportOpenHelperFactory(passphrase)
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DB_NAME,
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
