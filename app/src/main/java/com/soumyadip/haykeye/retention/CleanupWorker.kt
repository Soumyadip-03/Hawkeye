package com.soumyadip.haykeye.retention

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soumyadip.haykeye.classifier.ClassifierConfigRepository
import com.soumyadip.haykeye.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CleanupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext
            val configRepo = ClassifierConfigRepository(context)
            val config = configRepo.getConfig()

            val retentionDays = config.retentionDays
            if (retentionDays <= 0) {
                Log.d(TAG, "Database retention is disabled (retentionDays = $retentionDays). Skipping cleanup.")
                return@withContext Result.success()
            }

            val retentionMillis = retentionDays.toLong() * 24 * 60 * 60 * 1000L
            val cutoffTimestamp = System.currentTimeMillis() - retentionMillis

            val dao = AppDatabase.getDatabase(context).urgentMessageDao()
            val deletedCount = dao.deleteOlderThan(cutoffTimestamp)

            Log.d(
                TAG,
                "Auto-retention cleanup complete: Deleted $deletedCount messages older than $retentionDays days (cutoff: $cutoffTimestamp).",
            )
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing auto-retention database cleanup: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HaykeyeCleanupWorker"
    }
}
