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
class WhatsAppParserTest {

    @Test
    fun testParseIndividualWhatsAppMessage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val person = Person.Builder().setName("Alice").build()
        val notification = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle("Alice")
            .setContentText("Hello World")
            .setStyle(
                NotificationCompat.MessagingStyle(person)
                    .addMessage("Hello World", System.currentTimeMillis(), person),
            )
            .build()

        val sbn = StatusBarNotification(
            "com.whatsapp",
            "com.whatsapp",
            1,
            "tag",
            1000,
            1000,
            1,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = WhatsAppParser.parse(sbn)
        assertEquals(1, messages.size)

        val msg = messages[0]
        assertEquals("WhatsApp", msg.provider)
        assertEquals("Alice", msg.senderName)
        assertEquals(null, msg.groupName)
        assertEquals("Hello World", msg.fullText)
    }

    @Test
    fun testParseGroupWhatsAppMessage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val senderPerson = Person.Builder().setName("Bob").build()
        val userPerson = Person.Builder().setName("Me").build()

        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle("Family Group")
            .setGroupConversation(true)
            .addMessage("Emergency at home", System.currentTimeMillis(), senderPerson)

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setStyle(messagingStyle)
            .build()

        val sbn = StatusBarNotification(
            "com.whatsapp",
            "com.whatsapp",
            2,
            "tag",
            1000,
            1000,
            2,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = WhatsAppParser.parse(sbn)
        assertEquals(1, messages.size)

        val msg = messages[0]
        assertEquals("WhatsApp", msg.provider)
        assertEquals("Bob", msg.senderName)
        assertEquals("Family Group", msg.groupName)
        assertEquals("Emergency at home", msg.fullText)
    }

    @Test
    fun testSkipSummaryNotifications() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val notification = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle("WhatsApp")
            .setContentText("3 messages from 2 chats")
            .setGroupSummary(true)
            .build()

        val sbn = StatusBarNotification(
            "com.whatsapp",
            "com.whatsapp",
            3,
            "tag",
            1000,
            1000,
            3,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis(),
        )

        val messages = WhatsAppParser.parse(sbn)
        assertTrue("Summary notification should be skipped", messages.isEmpty())
    }
}
