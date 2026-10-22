package com.soumyadip.haykeye.classifier

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soumyadip.haykeye.data.AppDatabase
import com.soumyadip.haykeye.data.UrgentMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassifierIntegrationTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testNonUrgentMessageNotSavedToRoom() = runBlocking {
        val dao = db.urgentMessageDao()

        val ordinaryMessage = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Friend",
            preview = "Hey, good morning!",
            fullText = "Hey, good morning! Hope you have a great day.",
            timestamp = System.currentTimeMillis(),
        )

        val classification = UrgencyClassifier.classify(ordinaryMessage)
        if (classification.isUrgent) {
            dao.insertMessage(ordinaryMessage)
        }

        val allMessages = dao.getAllMessages().first()
        assertTrue("Non-urgent message should NOT be inserted into Room database", allMessages.isEmpty())
    }

    @Test
    fun testUrgentMessageSavedAndDeduplicated() = runBlocking {
        val dao = db.urgentMessageDao()

        val urgentMessage = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Boss",
            preview = "ASAP call me",
            fullText = "ASAP call me back when you get this",
            timestamp = 10000L,
        )

        val classification1 = UrgencyClassifier.classify(urgentMessage)
        assertTrue(classification1.isUrgent)

        val id1 = dao.insertMessage(urgentMessage)
        assertTrue(id1 > 0)

        // Attempting duplicate insertion
        val id2 = dao.insertMessage(urgentMessage)
        assertEquals(-1L, id2)

        val allMessages = dao.getAllMessages().first()
        assertEquals(1, allMessages.size)
    }
}
