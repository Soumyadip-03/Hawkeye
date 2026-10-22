package com.soumyadip.haykeye.parser

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.soumyadip.haykeye.data.UrgentMessage

/**
 * Parser for Google Messages notifications.
 *
 * Extracts individual SMS/MMS/RCS messages, sender information, conversation titles,
 * and timestamps using MessagingStyle when available.
 */
object MessagesParser {

    private const val PACKAGE_MESSAGES = "com.google.android.apps.messaging"

    private val SUMMARY_REGEX = Regex(
        """^(\d+\s+new\s+messages?|\d+\s+messages?\s+from\s+\d+\s+chats?)$""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(sbn: StatusBarNotification): List<UrgentMessage> {
        if (sbn.packageName != PACKAGE_MESSAGES) {
            return emptyList()
        }

        val notification = sbn.notification ?: return emptyList()
        val extras = notification.extras ?: return emptyList()

        val isGroupSummary = ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) ||
                extras.getBoolean("android.isGroupSummary")
        if (isGroupSummary) {
            return emptyList()
        }

        val provider = "Google Messages"
        val parsedMessages = mutableListOf<UrgentMessage>()

        // 1. Prefer MessagingStyle
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if ((messagingStyle != null) && messagingStyle.messages.isNotEmpty()) {
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
                        wasMentioned = false,
                        dedupKey = dedupKey,
                    ),
                )
            }
            return parsedMessages
        }

        // 2. Fallback for flat extras
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
