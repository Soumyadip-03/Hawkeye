package com.soumyadip.haykeye.parser

import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class MessagesParserTest {

    @Test
    fun testParseIndividualGoogleMessagesNotification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val senderPerson = Person.Builder().setName("Bank Alert").build()
        val messagingStyle = NotificationCompat.MessagingStyle(senderPerson)
            .addMessage("Your OTP is 482910 for transaction", System.currentTimeMillis(), senderPerson)

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setStyle(messagingStyle)
            .build()

        val sbn = StatusBarNotification(
            "com.google.android.apps.messaging",
            "com.google.android.apps.messaging",
            200,
            "tag",
            1000,
            1000,
            1,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = MessagesParser.parse(sbn)
        assertEquals(1, messages.size)

        val msg = messages[0]
        assertEquals("Google Messages", msg.provider)
        assertEquals("Bank Alert", msg.senderName)
        assertEquals("Your OTP is 482910 for transaction", msg.fullText)
    }

    @Test
    fun testParseGroupGoogleMessagesNotification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val senderPerson = Person.Builder().setName("David").build()
        val userPerson = Person.Builder().setName("Me").build()

        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle("Project Alpha Group")
            .setGroupConversation(true)
            .addMessage("Please send the report ASAP", System.currentTimeMillis(), senderPerson)

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setStyle(messagingStyle)
            .build()

        val sbn = StatusBarNotification(
            "com.google.android.apps.messaging",
            "com.google.android.apps.messaging",
            201,
            "tag",
            1000,
            1000,
            2,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = MessagesParser.parse(sbn)
        assertEquals(1, messages.size)

        val msg = messages[0]
        assertEquals("Google Messages", msg.provider)
        assertEquals("David", msg.senderName)
        assertEquals("Project Alpha Group", msg.groupName)
        assertEquals("Please send the report ASAP", msg.fullText)
    }

    @Test
    fun testSkipSummaryGoogleMessagesNotification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle("Messages")
            .setContentText("2 new messages")
            .setGroupSummary(true)
            .build()

        val sbn = StatusBarNotification(
            "com.google.android.apps.messaging",
            "com.google.android.apps.messaging",
            202,
            "tag",
            1000,
            1000,
            3,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = MessagesParser.parse(sbn)
        assertTrue("Summary notification should be skipped", messages.isEmpty())
    }
}
