package com.soumyadip.haykeye.parser

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.soumyadip.haykeye.data.UrgentMessage

/**
 * Parser for WhatsApp and WhatsApp Business notifications.
 *
 * Extracts individual sender names, group names, message text, and timestamps
 * from MessagingStyle notifications, while filtering out generic batch summaries.
 */
object WhatsAppParser {

    private const val PACKAGE_WHATSAPP = "com.whatsapp"
    private const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    private val SUMMARY_REGEX = Regex(
        """^(\d+\s+new\s+messages?|\d+\s+messages?\s+from\s+\d+\s+chats?)$""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(sbn: StatusBarNotification): List<UrgentMessage> {
        val packageName = sbn.packageName ?: return emptyList()
        val provider = when (packageName) {
            PACKAGE_WHATSAPP -> "WhatsApp"
            PACKAGE_WHATSAPP_BUSINESS -> "WhatsApp Business"
            else -> return emptyList()
        }

        val notification = sbn.notification ?: return emptyList()
        val extras = notification.extras ?: return emptyList()

        // Skip group summaries (e.g. "X messages from Y chats")
        val isGroupSummary = ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) ||
                extras.getBoolean("android.isGroupSummary")
        if (isGroupSummary) {
            return emptyList()
        }

        val parsedMessages = mutableListOf<UrgentMessage>()

        // 1. Prefer NotificationCompat.MessagingStyle
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val isGroup = messagingStyle.isGroupConversation || extras.getBoolean("android.isGroupConversation")
            val groupName = if (isGroup) {
                messagingStyle.conversationTitle?.toString()
                    ?: extras.getCharSequence(NotificationCompat.EXTRA_CONVERSATION_TITLE)?.toString()
            } else null

            for (msg in messagingStyle.messages) {
                val text = msg.text?.toString()?.trim() ?: continue
                if (text.isBlank() || SUMMARY_REGEX.matches(text)) {
                    continue
                }

                val senderName = msg.person?.name?.toString()
                    ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    ?: "Unknown"

                val timestamp = if (msg.timestamp > 0) msg.timestamp else sbn.postTime
                val preview = if (text.length > 100) text.take(100) + "..." else text

                // Limitation Note: Notification MessagingStyle extras do not consistently expose
                // an explicit @mention boolean across all Android distributions; defaults to false.
                val wasMentioned = false

                val dedupKey = "$provider|${groupName ?: ""}|$senderName|$timestamp|$text"

                parsedMessages.add(
                    UrgentMessage(
                        provider = provider,
                        senderName = senderName,
                        groupName = groupName,
                        preview = preview,
                        fullText = text,
                        timestamp = timestamp,
                        isRead = false,
                        wasMentioned = wasMentioned,
                        dedupKey = dedupKey,
                    ),
                )
            }
            return parsedMessages
        }

        // 2. Fallback for ordinary extras if MessagingStyle is absent
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

        if (title.isNullOrBlank() || text.isNullOrBlank() || SUMMARY_REGEX.matches(text)) {
            return emptyList()
        }

        val timestamp = sbn.postTime
        val preview = if (text.length > 100) text.take(100) + "..." else text
        val dedupKey = "$provider||$title|$timestamp|$text"

        parsedMessages.add(
            UrgentMessage(
                provider = provider,
                senderName = title,
                groupName = null,
                preview = preview,
                fullText = text,
                timestamp = timestamp,
                isRead = false,
                wasMentioned = false,
                dedupKey = dedupKey,
            ),
        )

        return parsedMessages
    }
}
