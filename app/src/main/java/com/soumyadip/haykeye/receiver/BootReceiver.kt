package com.soumyadip.haykeye.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.soumyadip.haykeye.navigation.NotificationActionRegistry
import com.soumyadip.haykeye.retention.RetentionManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        Log.d(TAG, "BootReceiver received broadcast action: '$action'")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Reschedule WorkManager periodic cleanup worker
                RetentionManager.scheduleCleanupWorker(context)
                NotificationActionRegistry.clear()
                Log.d(TAG, "Successfully re-initialized WorkManager retention tasks on boot/update.")
            }
        }
    }

    companion object {
        private const val TAG = "HaykeyeBootReceiver"
    }
}
