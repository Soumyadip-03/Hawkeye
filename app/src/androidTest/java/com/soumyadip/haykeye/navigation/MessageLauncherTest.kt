package com.soumyadip.haykeye.navigation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soumyadip.haykeye.MainActivity
import com.soumyadip.haykeye.data.UrgentMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageLauncherTest {

    @Test
    fun testTargetPackageMappingForSupportedProviders() {
        assertEquals("com.whatsapp", MessageLauncher.getTargetPackageName("WhatsApp"))
        assertEquals("com.whatsapp.w4b", MessageLauncher.getTargetPackageName("WhatsApp Business"))
        assertEquals("com.google.android.gm", MessageLauncher.getTargetPackageName("Gmail"))
        assertEquals("com.google.android.apps.messaging", MessageLauncher.getTargetPackageName("Google Messages"))
    }

    @Test
    fun testTargetPackageMappingForUnknownProvider() {
        assertNull(MessageLauncher.getTargetPackageName("UnknownProvider"))
        assertNull(MessageLauncher.getTargetPackageName("Telegram"))
    }

    @Test
    fun testContactMappingRepository() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = ContactMappingRepository(context)

        repo.saveContactMapping("John", "+919876543210")
        assertEquals("+919876543210", repo.getPhoneNumberForSender("John"))
        assertEquals("+919876543210", repo.getPhoneNumberForSender("john"))

        repo.removeContactMapping("John")
        assertNull(repo.getPhoneNumberForSender("John"))
    }

    @Test
    fun testPriority1ExactNotificationAction() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val dedupKey = "WhatsApp||Alice|1000|Hello"

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val message = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alice",
            preview = "Hello",
            fullText = "Hello",
            timestamp = 1000L,
            dedupKey = dedupKey,
        )

        NotificationActionRegistry.registerAction(message, pendingIntent)
        assertEquals(NotificationActionRegistry.getPendingIntent(message), pendingIntent)

        val result = MessageLauncher.launchSourceApp(context, message)
        assertTrue(
            "Priority 1 should return EXACT_NOTIFICATION_ACTION or MEDIA_VIEWER_ACTION",
            (result == LaunchResult.EXACT_NOTIFICATION_ACTION) || (result == LaunchResult.MEDIA_VIEWER_ACTION),
        )

        NotificationActionRegistry.clear()
    }

    @Test
    fun testPriority2DeepLinkWithPhoneNumber() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val dedupKey = "Google Messages||+1234567890|3000|SMS Text"

        val message = UrgentMessage(
            provider = "Google Messages",
            senderName = "+1234567890",
            preview = "SMS Text",
            fullText = "SMS Text",
            timestamp = 3000L,
            dedupKey = dedupKey,
        )

        NotificationActionRegistry.unregisterAction(message)

        val result = MessageLauncher.launchSourceApp(context, message)
        assertTrue(
            "Phone number sender should trigger CONTACT_OR_COMPOSE_DEEP_LINK, or APP_FALLBACK/APP_NOT_INSTALLED",
            (result == LaunchResult.CONTACT_OR_COMPOSE_DEEP_LINK) ||
                    (result == LaunchResult.APP_FALLBACK) ||
                    (result == LaunchResult.APP_NOT_INSTALLED),
        )
    }

    @Test
    fun testPriority3FallbackWhenNotificationActionIsDismissedAndSenderIsName() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val dedupKey = "WhatsApp||Bob|2000|Hi"

        val message = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Bob",
            preview = "Hi",
            fullText = "Hi",
            timestamp = 2000L,
            dedupKey = dedupKey,
        )

        NotificationActionRegistry.unregisterAction(message)
        assertNull(NotificationActionRegistry.getPendingIntent(message))

        val result = MessageLauncher.launchSourceApp(context, message)
        assertTrue(
            "When notification action is dismissed/missing and sender is a name, should fall back to APP_FALLBACK or APP_NOT_INSTALLED",
            (result == LaunchResult.APP_FALLBACK) || (result == LaunchResult.APP_NOT_INSTALLED),
        )
    }

    @Test
    fun testLaunchForUnknownProviderReturnsFailedWithoutCrashing() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val unknownMessage = UrgentMessage(
            provider = "UnknownApp",
            senderName = "Test",
            preview = "Test",
            fullText = "Test",
            timestamp = System.currentTimeMillis(),
        )

        val result = MessageLauncher.launchSourceApp(context, unknownMessage)
        assertEquals(LaunchResult.FAILED, result)
    }

    @Test
    fun testLaunchForUninstalledAppDoesNotCrash() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val fakeAppMessage = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Test",
            preview = "Test",
            fullText = "Test",
            timestamp = System.currentTimeMillis(),
        )

        val result = MessageLauncher.launchSourceApp(context, fakeAppMessage)
        assertTrue(
            "Result must be a valid LaunchResult without crashing",
            (result == LaunchResult.EXACT_NOTIFICATION_ACTION) ||
                    (result == LaunchResult.MEDIA_VIEWER_ACTION) ||
                    (result == LaunchResult.MAPPED_CHAT_DEEP_LINK) ||
                    (result == LaunchResult.CONTACT_OR_COMPOSE_DEEP_LINK) ||
                    (result == LaunchResult.APP_FALLBACK) ||
                    (result == LaunchResult.APP_NOT_INSTALLED) ||
                    (result == LaunchResult.FAILED),
        )
    }
}
