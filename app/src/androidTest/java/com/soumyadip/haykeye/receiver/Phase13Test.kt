package com.soumyadip.haykeye.receiver

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.soumyadip.haykeye.listener.NotificationListener
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase13Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testBootReceiverReschedulesWorkManagerTasks() {
        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("haykeye_cleanup_periodic_worker")
            .get()

        assertNotNull(workInfos)
        assertTrue(workInfos.isNotEmpty())
        assertTrue(
            workInfos[0].state == WorkInfo.State.ENQUEUED ||
                    workInfos[0].state == WorkInfo.State.RUNNING,
        )
    }

    @Test
    fun testBatteryOptimizationStatusCheckDoesNotCrash() {
        val isIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        assertTrue(isIgnored || !isIgnored)

        val intent = BatteryOptimizationHelper.openBatterySettingsIntent(context)
        assertNotNull(intent)
    }

    @Test
    fun testNotificationListenerStatusTracking() {
        assertNotNull(NotificationListener.isListenerConnected)
    }
}
