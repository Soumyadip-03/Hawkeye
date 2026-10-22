package com.soumyadip.haykeye.security

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soumyadip.haykeye.data.AppDatabase
import com.soumyadip.haykeye.data.UrgentMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSecurityTest {

    private lateinit var context: Context
    private val testDbName = "test_encrypted_security.db"
    private val correctPassphrase = "correct_secret_passphrase_32_bytes!".toByteArray()
    private val wrongPassphrase = "wrong_secret_passphrase_32_bytes!!".toByteArray()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(testDbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(testDbName)
    }

    @Test
    fun testEncryptedDatabaseOpensWithCorrectKey() = runBlocking {
        val factory = SupportOpenHelperFactory(correctPassphrase)
        val db = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
            .openHelperFactory(factory)
            .build()

        val dao = db.urgentMessageDao()
        val message = UrgentMessage(
            provider = "WhatsApp",
            senderName = "SecTest",
            preview = "Encrypted Test",
            fullText = "Encrypted Test Full Body",
            timestamp = System.currentTimeMillis(),
        )

        val id = dao.insertMessage(message)
        assertTrue(id > 0)

        val list = dao.getAllMessages().first()
        assertEquals(1, list.size)
        assertEquals("SecTest", list[0].senderName)

        db.close()
    }

    @Test
    fun testOpeningDatabaseWithWrongKeyFails() = runBlocking {
        // Step 1: Create database with correct passphrase
        val factory1 = SupportOpenHelperFactory(correctPassphrase)
        val db1 = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
            .openHelperFactory(factory1)
            .build()

        db1.urgentMessageDao().insertMessage(
            UrgentMessage(
                provider = "Gmail",
                senderName = "Secret",
                preview = "Secret Message",
                fullText = "Secret Message",
                timestamp = System.currentTimeMillis(),
            ),
        )
        db1.close()

        // Step 2: Attempt to open the encrypted database with wrong passphrase
        var failed = false
        try {
            val factory2 = SupportOpenHelperFactory(wrongPassphrase)
            val db2 = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
                .openHelperFactory(factory2)
                .build()

            // Trigger database open
            db2.urgentMessageDao().getAllMessages().first()
            db2.close()
        } catch (_: Exception) {
            failed = true
        }

        assertTrue("Opening encrypted database with wrong passphrase MUST fail", failed)
    }

    @Test
    fun testPlaintextToEncryptedMigrationPreservesRecords() = runBlocking {
        val migrationDbName = "test_plaintext_to_encrypted.db"
        context.deleteDatabase(migrationDbName)

        try {
            // Step 1: Create a plain unencrypted Room database and insert test records
            val plainDb = Room.databaseBuilder(context, AppDatabase::class.java, migrationDbName).build()
            val plainDao = plainDb.urgentMessageDao()

            val msg1 = UrgentMessage(
                provider = "WhatsApp",
                senderName = "Alice",
                preview = "Msg 1",
                fullText = "Msg 1",
                timestamp = 1000L,
            )
            val msg2 = UrgentMessage(
                provider = "Gmail",
                senderName = "Bob",
                preview = "Msg 2",
                fullText = "Msg 2",
                timestamp = 2000L,
            )

            plainDao.insertMessage(msg1)
            plainDao.insertMessage(msg2)
            plainDb.close()

            // Verify database is plaintext
            assertTrue(DatabaseMigrator.isDatabasePlaintext(context, migrationDbName))

            // Step 2: Execute plaintext to SQLCipher migration
            val migrationSuccess = DatabaseMigrator.migratePlaintextToEncrypted(context, migrationDbName, correctPassphrase)
            assertTrue("Migration should succeed", migrationSuccess)

            // Database should no longer be plaintext
            assertFalse(DatabaseMigrator.isDatabasePlaintext(context, migrationDbName))

            // Step 3: Open migrated database with SQLCipher SupportOpenHelperFactory and verify data
            val factory = SupportOpenHelperFactory(correctPassphrase)
            val encryptedDb = Room.databaseBuilder(context, AppDatabase::class.java, migrationDbName)
                .openHelperFactory(factory)
                .build()

            val migratedList = encryptedDb.urgentMessageDao().getAllMessages().first()
            assertEquals(2, migratedList.size)
            assertEquals("Bob", migratedList[0].senderName) // Sorted timestamp DESC
            assertEquals("Alice", migratedList[1].senderName)

            encryptedDb.close()
        } finally {
            context.deleteDatabase(migrationDbName)
        }
    }
}
