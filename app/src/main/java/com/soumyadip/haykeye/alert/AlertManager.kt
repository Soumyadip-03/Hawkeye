package com.soumyadip.haykeye.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.soumyadip.haykeye.MainActivity
import com.soumyadip.haykeye.data.UrgentMessage

object AlertManager {

    private const val TAG = "HaykeyeAlertManager"
    const val CHANNEL_ID = "haykeye_urgent_alerts"
    private const val NOTIFICATION_ID_BASE = 1000

    private var activeRingtone: Ringtone? = null

    fun triggerUrgentAlert(context: Context, message: UrgentMessage) {
        val appContext = context.applicationContext

        // 1. Ensure Notification Channel exists
        createNotificationChannel(appContext)

        // 2. Play ringtone safely off-thread
        playAlertSound(appContext)

        // 3. Post System Notification safely
        postAlertNotification(appContext, message)
    }

    fun createNotificationChannel(context: Context) {
        val notificationManager = (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?: return

        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Urgent Message Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "High importance alerts when an urgent message is detected"
                // Set channel sound to null so the OS doesn't play a second default sound
                setSound(null, null)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun playAlertSound(context: Context) {
        try {
            activeRingtone?.stop()

            val prefs = AlertPreferences(context)
            val ringtoneUri = prefs.getRingtoneUri(context)

            val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
            if (ringtone != null) {
                activeRingtone = ringtone
                ringtone.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alert sound: ${e.message}")
        }
    }

    private fun postAlertNotification(context: Context, message: UrgentMessage) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skipping system notification.")
                    return
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Haykeye Urgent Alert")
                .setContentText("A new urgent message was detected from ${message.provider}.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((NOTIFICATION_ID_BASE + (message.id % 1000)).toInt(), notification)
            Log.d(TAG, "Posted urgent notification alert for message ID ${message.id}")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException posting notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification: ${e.message}")
        }
    }
}
