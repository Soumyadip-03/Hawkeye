package com.soumyadip.haykeye.alert

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soumyadip.haykeye.classifier.UrgencyClassifier
import com.soumyadip.haykeye.data.UrgentMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlertManagerTest {

    @Test
    fun testRingtoneUriFallbackForInvalidUri() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = AlertPreferences(context)

        val invalidUri = Uri.parse("content://invalid/path/to/nonexistent_sound")
        prefs.saveRingtoneUri(invalidUri)

        val resolvedUri = prefs.getRingtoneUri(context)
        assertNotNull("Resolved URI must not be null", resolvedUri)
        assertTrue("Resolved URI should fall back to default URI", resolvedUri != Uri.EMPTY)

        prefs.saveRingtoneUri(null)
    }

    @Test
    fun testCreateNotificationChannelDoesNotCrash() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        AlertManager.createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = notificationManager.getNotificationChannel(AlertManager.CHANNEL_ID)

        assertNotNull("Notification channel should be created", channel)
        assertEquals("Urgent Message Alerts", channel.name)
    }

    @Test
    fun testUrgentMessageClassificationTriggersAlertLogic() {
        val urgentMessage = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Boss",
            preview = "ASAP call me",
            fullText = "ASAP call me",
            timestamp = System.currentTimeMillis(),
        )

        val nonUrgentMessage = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Friend",
            preview = "Good morning",
            fullText = "Good morning",
            timestamp = System.currentTimeMillis(),
        )

        val urgentResult = UrgencyClassifier.classify(urgentMessage)
        val nonUrgentResult = UrgencyClassifier.classify(nonUrgentMessage)

        assertTrue("Urgent message should be classified as urgent", urgentResult.isUrgent)
        assertFalse("Non-urgent message should NOT be classified as urgent", nonUrgentResult.isUrgent)
    }
}
