package com.soumyadip.haykeye.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UrgentMessage::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun urgentMessageDao(): UrgentMessageDao

    companion object {
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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "haykeye_database",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
