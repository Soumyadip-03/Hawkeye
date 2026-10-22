package com.soumyadip.haykeye.retention

import android.content.Context
import android.text.format.Formatter
import com.soumyadip.haykeye.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseSizeCalculator {

    suspend fun calculateFormattedSize(context: Context): String = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val dao = AppDatabase.getDatabase(appContext).urgentMessageDao()
        val count = dao.getMessageCount()

        if (count == 0) {
            return@withContext "0 KB (0 messages)"
        }

        val dbFile = appContext.getDatabasePath(AppDatabase.DB_NAME)
        val walFile = appContext.getDatabasePath("${AppDatabase.DB_NAME}-wal")
        val shmFile = appContext.getDatabasePath("${AppDatabase.DB_NAME}-shm")

        var totalBytes = 0L
        if (dbFile.exists()) totalBytes += dbFile.length()
        if (walFile.exists()) totalBytes += walFile.length()
        if (shmFile.exists()) totalBytes += shmFile.length()

        if (totalBytes <= 0L) {
            "0 KB"
        } else {
            "${Formatter.formatShortFileSize(appContext, totalBytes)} ($count messages)"
        }
    }
}
