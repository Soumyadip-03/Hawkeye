package com.soumyadip.haykeye.parser

import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class GmailParserTest {

    @Test
    fun testParseNormalGmailWithSubject() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle("GitHub")
            .setContentText("[Security Alert] New sign-in detected")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("[Security Alert] New sign-in detected from Windows device in Kolkata"),
            )
            .build()

        val sbn = StatusBarNotification(
            "com.google.android.gm",
            "com.google.android.gm",
            100,
            "tag",
            1000,
            1000,
            1,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = GmailParser.parse(sbn)
        assertEquals(1, messages.size)

        val msg = messages[0]
        assertEquals("Gmail", msg.provider)
        assertEquals("GitHub", msg.senderName)
        assertEquals("[Security Alert] New sign-in detected from Windows device in Kolkata", msg.fullText)
    }

    @Test
    fun testParseGmailWithNoSubjectButBodyPreview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle("John Doe")
            .setContentText("")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Hey, please check the attached project document when you get a chance."),
            )
            .build()

        val sbn = StatusBarNotification(
            "com.google.android.gm",
            "com.google.android.gm",
            101,
            "tag",
            1000,
            1000,
            1,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = GmailParser.parse(sbn)
        assertEquals(1, messages.size)

        val msg = messages[0]
        assertEquals("Gmail", msg.provider)
        assertEquals("John Doe", msg.senderName)
        assertEquals("Hey, please check the attached project document when you get a chance.", msg.fullText)
    }

    @Test
    fun testParseGmailWithNoSubjectAndNoPreview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle("Jane Smith")
            .setContentText("   ")
            .build()

        val sbn = StatusBarNotification(
            "com.google.android.gm",
            "com.google.android.gm",
            102,
            "tag",
            1000,
            1000,
            1,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = GmailParser.parse(sbn)
        assertEquals(1, messages.size)

        val msg = messages[0]
        assertEquals("Gmail", msg.provider)
        assertEquals("Jane Smith", msg.senderName)
        assertEquals("No subject", msg.fullText)
        assertEquals("No subject", msg.preview)
    }

    @Test
    fun testSkipGmailWithMissingSender() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle("   ")
            .setContentText("Some body text without a sender")
            .build()

        val sbn = StatusBarNotification(
            "com.google.android.gm",
            "com.google.android.gm",
            103,
            "tag",
            1000,
            1000,
            1,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = GmailParser.parse(sbn)
        assertTrue("Notification without sender info should be skipped", messages.isEmpty())
    }

    @Test
    fun testSkipGmailBatchSummary() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle("Gmail")
            .setContentText("3 new messages")
            .setGroupSummary(true)
            .build()

        val sbn = StatusBarNotification(
            "com.google.android.gm",
            "com.google.android.gm",
            104,
            "tag",
            1000,
            1000,
            1,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = GmailParser.parse(sbn)
        assertTrue("Batch summary should be skipped", messages.isEmpty())
    }
}
