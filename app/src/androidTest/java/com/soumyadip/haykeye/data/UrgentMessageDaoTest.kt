package com.soumyadip.haykeye.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class UrgentMessageDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: UrgentMessageDao

    @Before
    fun createDb() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.urgentMessageDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveMessage() = runBlocking {
        val message = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alice",
            groupName = "Family",
            preview = "Emergency at home!",
            fullText = "Emergency at home! Please call me ASAP.",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            wasMentioned = true,
        )

        val insertedId = dao.insertMessage(message)
        assertTrue("Inserted ID should be > 0", insertedId > 0)

        val allMessages = dao.getAllMessages().first()
        assertEquals(1, allMessages.size)

        val retrieved = allMessages[0]
        assertEquals("WhatsApp", retrieved.provider)
        assertEquals("Alice", retrieved.senderName)
        assertEquals("Family", retrieved.groupName)
        assertEquals("Emergency at home!", retrieved.preview)
        assertFalse(retrieved.isRead)
        assertTrue(retrieved.wasMentioned)
    }

    @Test
    fun testFilterByProvider() = runBlocking {
        val message1 = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Bob",
            preview = "Need help ASAP",
            fullText = "Need help ASAP",
            timestamp = 1000L,
        )
        val message2 = UrgentMessage(
            provider = "Gmail",
            senderName = "Boss",
            preview = "Urgent project update",
            fullText = "Urgent project update",
            timestamp = 2000L,
        )

        val id1 = dao.insertMessage(message1)
        val id2 = dao.insertMessage(message2)

        assertTrue("Message 1 inserted ID should be > 0", id1 > 0)
        assertTrue("Message 2 inserted ID should be > 0", id2 > 0)

        val whatsappMessages = dao.getMessagesByProvider("WhatsApp").first()
        assertEquals(1, whatsappMessages.size)
        assertEquals("Bob", whatsappMessages[0].senderName)

        val gmailMessages = dao.getMessagesByProvider("Gmail").first()
        assertEquals(1, gmailMessages.size)
        assertEquals("Boss", gmailMessages[0].senderName)
    }

    @Test
    fun testDuplicateMessagesAreIgnored() = runBlocking {
        val message1 = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alice",
            preview = "Duplicate test message",
            fullText = "Duplicate test message full text",
            timestamp = 5000L,
        )
        val messageDuplicate = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alice",
            preview = "Duplicate test message",
            fullText = "Duplicate test message full text",
            timestamp = 5000L,
        )

        val firstInsertId = dao.insertMessage(message1)
        assertTrue("First insert should succeed", firstInsertId > 0)

        val duplicateInsertId = dao.insertMessage(messageDuplicate)
        assertEquals("Duplicate insert should be ignored and return -1", -1L, duplicateInsertId)

        val allMessages = dao.getAllMessages().first()
        assertEquals("Database should contain exactly 1 message", 1, allMessages.size)
    }

    @Test
    fun testMarkAsReadAndDelete() = runBlocking {
        val message = UrgentMessage(
            provider = "Google Messages",
            senderName = "12345",
            preview = "Security code",
            fullText = "Your security code is 9999",
            timestamp = System.currentTimeMillis(),
        )

        val id = dao.insertMessage(message)
        dao.markRead(id = id, isRead = true)

        val updatedList = dao.getAllMessages().first()
        assertTrue(updatedList[0].isRead)

        dao.deleteMessage(updatedList[0])
        val emptyList = dao.getAllMessages().first()
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun testFileBackedDatabasePersistence() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testDbName = "test_persistent_haykeye.db"

        context.deleteDatabase(testDbName)

        try {
            val db1 = Room.databaseBuilder(context, AppDatabase::class.java, testDbName).build()
            val dao1 = db1.urgentMessageDao()

            val testMessage = UrgentMessage(
                provider = "WhatsApp",
                senderName = "Charlie",
                preview = "Persistent test message",
                fullText = "Persistent test message full content",
                timestamp = System.currentTimeMillis(),
            )

            val insertedId = dao1.insertMessage(testMessage)
            assertTrue("Inserted ID should be > 0", insertedId > 0)

            db1.close()

            val db2 = Room.databaseBuilder(context, AppDatabase::class.java, testDbName).build()
            val dao2 = db2.urgentMessageDao()

            val retrievedList = dao2.getAllMessages().first()
            assertEquals(1, retrievedList.size)

            val retrievedMessage = retrievedList[0]
            assertEquals("WhatsApp", retrievedMessage.provider)
            assertEquals("Charlie", retrievedMessage.senderName)
            assertEquals("Persistent test message", retrievedMessage.preview)

            db2.close()
        } finally {
            context.deleteDatabase(testDbName)
        }
    }

    @Test
    fun testMigrationFromVersion1To2() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testMigrationDbName = "test_migration_haykeye.db"

        context.deleteDatabase(testMigrationDbName)

        try {
            val factory = FrameworkSQLiteOpenHelperFactory()
            val helper = factory.create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(testMigrationDbName)
                    .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS urgent_messages (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    provider TEXT NOT NULL,
                                    senderName TEXT NOT NULL,
                                    groupName TEXT,
                                    preview TEXT NOT NULL,
                                    fullText TEXT NOT NULL,
                                    timestamp INTEGER NOT NULL,
                                    isRead INTEGER NOT NULL,
                                    wasMentioned INTEGER NOT NULL
                                )
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(
                            db: androidx.sqlite.db.SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {}
                    })
                    .build(),
            )

            val v1Db = helper.writableDatabase
            v1Db.execSQL("INSERT INTO urgent_messages VALUES (1, 'WhatsApp', 'User A', NULL, 'Text 1', 'Text 1', 1000, 0, 0)")
            v1Db.execSQL("INSERT INTO urgent_messages VALUES (2, 'Gmail', 'User B', NULL, 'Text 2', 'Text 2', 2000, 0, 0)")
            v1Db.close()

            val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, testMigrationDbName)
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .build()

            val migratedDao = migratedDb.urgentMessageDao()
            val migratedList = migratedDao.getAllMessages().first()

            assertEquals(2, migratedList.size)
            migratedDb.close()
        } finally {
            context.deleteDatabase(testMigrationDbName)
        }
    }
}
