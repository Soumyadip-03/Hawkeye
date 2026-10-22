package com.soumyadip.haykeye.parser

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.soumyadip.haykeye.data.UrgentMessage

/**
 * Parser for Gmail notifications.
 *
 * Extracts sender name, available subject or body snippet text, and timestamp
 * from Gmail notifications while skipping batch summaries and handling subjectless emails safely.
 */
object GmailParser {

    private const val PACKAGE_GMAIL = "com.google.android.gm"

    private val SUMMARY_REGEX = Regex(
        """^(\d+\s+new\s+(messages?|emails?)(|\s+from\s+.*)|new\s+messages?)$""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(sbn: StatusBarNotification): List<UrgentMessage> {
        if (sbn.packageName != PACKAGE_GMAIL) {
            return emptyList()
        }

        val notification = sbn.notification ?: return emptyList()
        val extras = notification.extras ?: return emptyList()

        // 1. Skip batch summary notifications (e.g. "3 new messages" or FLAG_GROUP_SUMMARY)
        val isGroupSummary = ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) ||
                extras.getBoolean("android.isGroupSummary")
        if (isGroupSummary) {
            return emptyList()
        }

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val rawSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val rawBigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val rawTextLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)

        // 2. Determine sender name; if completely missing or blank, skip notification
        val senderName = when {
            !rawTitle.isNullOrBlank() -> rawTitle
            !rawSubText.isNullOrBlank() -> rawSubText
            else -> return emptyList()
        }

        // 3. Skip if sender or title matches batch summary pattern
        if (SUMMARY_REGEX.matches(senderName) || ((rawText != null) && SUMMARY_REGEX.matches(rawText))) {
            return emptyList()
        }

        // 4. Extract body/subject text safely
        val availableText = when {
            !rawBigText.isNullOrBlank() -> rawBigText
            !rawText.isNullOrBlank() -> rawText
            !rawTextLines.isNullOrEmpty() -> rawTextLines.joinToString("\n") { it?.toString()?.trim() ?: "" }.trim()
            else -> ""
        }

        // Fallback to "No subject" when subject and body snippet are empty/blank
        val fullText = availableText.ifBlank { "No subject" }
        val preview = if (fullText.length > 100) fullText.take(100) + "..." else fullText

        val timestamp = sbn.postTime
        val provider = "Gmail"
        val dedupKey = "$provider||$senderName|$timestamp|$fullText"

        return listOf(
            UrgentMessage(
                provider = provider,
                senderName = senderName,
                groupName = null,
                preview = preview,
                fullText = fullText,
                timestamp = timestamp,
                isRead = false,
                wasMentioned = false,
                dedupKey = dedupKey,
            ),
        )
    }
}
