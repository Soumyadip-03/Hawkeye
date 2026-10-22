package com.soumyadip.haykeye.listener

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.soumyadip.haykeye.alert.AlertManager
import com.soumyadip.haykeye.classifier.ClassifierConfigRepository
import com.soumyadip.haykeye.classifier.UrgencyClassifier
import com.soumyadip.haykeye.data.AppDatabase
import com.soumyadip.haykeye.navigation.NotificationActionRegistry
import com.soumyadip.haykeye.parser.GmailParser
import com.soumyadip.haykeye.parser.MessagesParser
import com.soumyadip.haykeye.parser.WhatsAppParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var configRepository: ClassifierConfigRepository

    override fun onCreate() {
        super.onCreate()
        configRepository = ClassifierConfigRepository(applicationContext)
        AlertManager.createNotificationChannel(applicationContext)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isListenerConnected = true
        Log.d(TAG, "Notification listener connected successfully")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isListenerConnected = false
        Log.w(TAG, "Notification listener disconnected. Requesting system rebind...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, NotificationListener::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request listener rebind: ${e.message}")
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val packageName = sbn?.packageName ?: return
        val providerName = SUPPORTED_PACKAGES[packageName] ?: return

        val config = configRepository.getConfig()
        if (!config.isListeningEnabled) {
            Log.d(TAG, "Haykeye notification listener is paused by Master Switch. Skipping message.")
            return
        }

        val messages = when (packageName) {
            PACKAGE_WHATSAPP, PACKAGE_WHATSAPP_BUSINESS -> WhatsAppParser.parse(sbn)
            PACKAGE_GMAIL -> GmailParser.parse(sbn)
            PACKAGE_MESSAGES -> MessagesParser.parse(sbn)
            else -> emptyList()
        }

        if (messages.isEmpty()) {
            return
        }

        val contentIntent = sbn.notification?.contentIntent

        if (contentIntent != null) {
            for (msg in messages) {
                NotificationActionRegistry.registerAction(msg, contentIntent)
            }
        }

        val dao = AppDatabase.getDatabase(applicationContext).urgentMessageDao()

        serviceScope.launch {
            for (msg in messages) {
                val classification = UrgencyClassifier.classify(msg, config)

                if (classification.isUrgent) {
                    val insertedId = dao.insertMessage(msg)
                    if (insertedId != -1L) {
                        Log.d(
                            TAG,
                            "[$providerName] Classification: URGENT (Signal: ${classification.matchedSignal}) from '${msg.senderName}' | Row ID: $insertedId",
                        )
                        val savedMessage = msg.copy(id = insertedId)
                        AlertManager.triggerUrgentAlert(applicationContext, savedMessage)
                    } else {
                        Log.d(
                            TAG,
                            "[$providerName] Classification: URGENT but skipped duplicate message from '${msg.senderName}'",
                        )
                    }
                } else {
                    Log.d(
                        TAG,
                        "[$providerName] Classification: NOT URGENT (Skipped) from '${msg.senderName}'",
                    )
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        val packageName = sbn?.packageName ?: return
        if (!SUPPORTED_PACKAGES.containsKey(packageName)) return

        val messages = when (packageName) {
            PACKAGE_WHATSAPP, PACKAGE_WHATSAPP_BUSINESS -> WhatsAppParser.parse(sbn)
            PACKAGE_GMAIL -> GmailParser.parse(sbn)
            PACKAGE_MESSAGES -> MessagesParser.parse(sbn)
            else -> emptyList()
        }

        for (msg in messages) {
            NotificationActionRegistry.unregisterAction(msg)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isListenerConnected = false
        serviceScope.cancel()
        NotificationActionRegistry.clear()
    }

    companion object {
        private const val TAG = "HaykeyeListener"

        @Volatile
        var isListenerConnected: Boolean = false
            private set

        fun requestReconnect(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestRebind(ComponentName(context, NotificationListener::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to request manual rebind: ${e.message}")
                }
            }
        }

        private const val PACKAGE_WHATSAPP = "com.whatsapp"
        private const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"
        private const val PACKAGE_GMAIL = "com.google.android.gm"
        private const val PACKAGE_MESSAGES = "com.google.android.apps.messaging"

        val SUPPORTED_PACKAGES = mapOf(
            PACKAGE_WHATSAPP to "WhatsApp",
            PACKAGE_WHATSAPP_BUSINESS to "WhatsApp Business",
            PACKAGE_GMAIL to "Gmail",
            PACKAGE_MESSAGES to "Google Messages",
        )
    }
}
