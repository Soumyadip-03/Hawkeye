package com.soumyadip.haykeye.navigation

import android.app.PendingIntent
import com.soumyadip.haykeye.data.UrgentMessage
import java.util.concurrent.ConcurrentHashMap

object NotificationActionRegistry {

    private val pendingIntentsByDedupKey = ConcurrentHashMap<String, PendingIntent>()
    private val pendingIntentsByProviderAndSender = ConcurrentHashMap<String, PendingIntent>()

    fun registerAction(message: UrgentMessage, pendingIntent: PendingIntent?) {
        if (pendingIntent != null) {
            if (message.dedupKey.isNotBlank()) {
                pendingIntentsByDedupKey[message.dedupKey] = pendingIntent
            }
            val providerSenderKey = buildProviderSenderKey(message.provider, message.senderName)
            pendingIntentsByProviderAndSender[providerSenderKey] = pendingIntent
        }
    }

    fun unregisterAction(message: UrgentMessage) {
        if (message.dedupKey.isNotBlank()) {
            pendingIntentsByDedupKey.remove(message.dedupKey)
        }
        val providerSenderKey = buildProviderSenderKey(message.provider, message.senderName)
        pendingIntentsByProviderAndSender.remove(providerSenderKey)
    }

    fun getPendingIntent(message: UrgentMessage): PendingIntent? {
        // 1. Try exact dedupKey match first
        if (message.dedupKey.isNotBlank()) {
            val exactIntent = pendingIntentsByDedupKey[message.dedupKey]
            if (exactIntent != null) return exactIntent
        }

        // 2. Fallback to active notification for provider + senderName
        val providerSenderKey = buildProviderSenderKey(message.provider, message.senderName)
        return pendingIntentsByProviderAndSender[providerSenderKey]
    }

    fun getPendingIntentByDedupKey(dedupKey: String): PendingIntent? {
        if (dedupKey.isBlank()) return null
        return pendingIntentsByDedupKey[dedupKey]
    }

    fun clear() {
        pendingIntentsByDedupKey.clear()
        pendingIntentsByProviderAndSender.clear()
    }

    private fun buildProviderSenderKey(provider: String, senderName: String): String {
        return "$provider|${senderName.trim().lowercase()}"
    }
}
