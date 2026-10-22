package com.soumyadip.haykeye.ui

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soumyadip.haykeye.classifier.ClassifierConfig
import com.soumyadip.haykeye.classifier.ClassifierConfigRepository
import com.soumyadip.haykeye.classifier.GroupNoisePolicy
import com.soumyadip.haykeye.classifier.UrgencyClassifier
import com.soumyadip.haykeye.classifier.UrgencySignal
import com.soumyadip.haykeye.data.AppDatabase
import com.soumyadip.haykeye.data.UrgentMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase12Test {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testMessageReadUnreadAndBulkClear() = runBlocking {
        val dao = db.urgentMessageDao()

        val msg1 = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alice",
            preview = "Text 1",
            fullText = "Text 1",
            timestamp = 1000L,
            isRead = false,
            dedupKey = "W||Alice|1000|Text1",
        )
        val msg2 = UrgentMessage(
            provider = "Gmail",
            senderName = "Bob",
            preview = "Text 2",
            fullText = "Text 2",
            timestamp = 2000L,
            isRead = false,
            dedupKey = "G||Bob|2000|Text2",
        )

        val id1 = dao.insertMessage(msg1)
        val id2 = dao.insertMessage(msg2)

        // Mark msg1 as read
        dao.markRead(id1, true)
        val list1 = dao.getAllMessages().first()
        val readMsg = list1.find { it.id == id1 }
        assertTrue(readMsg!!.isRead)

        // Bulk clear read messages
        val deletedCount = dao.deleteReadMessages()
        assertEquals(1, deletedCount)

        val list2 = dao.getAllMessages().first()
        assertEquals(1, list2.size)
        assertEquals(id2, list2[0].id)
    }

    @Test
    fun testRetentionCutoffCleanup() = runBlocking {
        val dao = db.urgentMessageDao()
        val now = System.currentTimeMillis()
        val tenDaysAgo = now - (10L * 24 * 60 * 60 * 1000L)

        val oldMsg = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Old",
            preview = "Old Msg",
            fullText = "Old Msg",
            timestamp = tenDaysAgo,
            dedupKey = "W||Old|$tenDaysAgo|Old",
        )
        val newMsg = UrgentMessage(
            provider = "WhatsApp",
            senderName = "New",
            preview = "New Msg",
            fullText = "New Msg",
            timestamp = now,
            dedupKey = "W||New|$now|New",
        )

        dao.insertMessage(oldMsg)
        dao.insertMessage(newMsg)

        // 5-day retention cutoff
        val cutoff = now - (5L * 24 * 60 * 60 * 1000L)
        val deleted = dao.deleteOlderThan(cutoff)

        assertEquals(1, deleted)
        val remaining = dao.getAllMessages().first()
        assertEquals(1, remaining.size)
        assertEquals("New", remaining[0].senderName)
    }

    @Test
    fun testGroupNoiseControlPolicies() {
        val groupMessage = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Charlie",
            groupName = "Spam Group",
            preview = "urgent meeting now!!",
            fullText = "urgent meeting now!!",
            timestamp = System.currentTimeMillis(),
            wasMentioned = false,
        )

        // 1. Ignore entirely policy
        val configIgnore = ClassifierConfig(
            groupNoisePolicies = mapOf("WhatsApp|Spam Group" to GroupNoisePolicy.IGNORE_ENTIRELY),
        )
        val resultIgnore = UrgencyClassifier.classify(groupMessage, configIgnore)
        assertFalse(resultIgnore.isUrgent)
        assertNull(resultIgnore.matchedSignal)

        // 2. Alert on mention only policy (wasMentioned = false)
        val configMentionOnly = ClassifierConfig(
            groupNoisePolicies = mapOf("WhatsApp|Spam Group" to GroupNoisePolicy.ALERT_ON_MENTION_ONLY),
        )
        val resultMentionFalse = UrgencyClassifier.classify(groupMessage, configMentionOnly)
        assertFalse(resultMentionFalse.isUrgent)

        // 3. Alert on mention only policy (wasMentioned = true)
        val groupMentioned = groupMessage.copy(wasMentioned = true)
        val resultMentionTrue = UrgencyClassifier.classify(groupMentioned, configMentionOnly)
        assertTrue(resultMentionTrue.isUrgent)
        assertEquals(UrgencySignal.MENTION, resultMentionTrue.matchedSignal)
    }

    @Test
    fun testSettingsPersistence() {
        val repo = ClassifierConfigRepository(context)

        val configToSave = ClassifierConfig(
            customKeywords = setOf("superimportant", "code123"),
            enableTextHeuristics = false,
            retentionDays = 30,
            vipSenders = mapOf("WhatsApp" to setOf("Boss")),
            groupNoisePolicies = mapOf("WhatsApp|Work" to GroupNoisePolicy.ALERT_ON_MENTION_ONLY),
        )

        repo.saveConfig(configToSave)

        val loadedConfig = repo.getConfig()
        assertTrue(loadedConfig.customKeywords.contains("superimportant"))
        assertFalse(loadedConfig.enableTextHeuristics)
        assertEquals(30, loadedConfig.retentionDays)
        assertTrue(loadedConfig.vipSenders["WhatsApp"]!!.contains("Boss"))
        assertEquals(GroupNoisePolicy.ALERT_ON_MENTION_ONLY, loadedConfig.groupNoisePolicies["WhatsApp|Work"])
    }
}
