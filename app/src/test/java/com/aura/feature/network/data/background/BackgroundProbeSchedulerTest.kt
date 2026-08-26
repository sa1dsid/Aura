package com.aura.feature.network.data.background

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.hours

private const val WORK_NAME = "aura-background-probe"

@RunWith(RobolectricTestRunner::class)
class BackgroundProbeSchedulerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun startWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setMinimumLoggingLevel(Log.DEBUG)
                .build(),
        )
    }

    @Test
    fun `nothing is booked until somebody schedules it`() {
        assertEquals(emptyList<WorkInfo>(), infos())
    }

    @Test
    fun `the probe is booked as one enqueued window`() {
        scheduler().schedule()

        assertEquals(WorkInfo.State.ENQUEUED, infos().single().state)
    }

    @Test
    fun `the probe comes back every twelve hours`() {
        scheduler().schedule()

        assertEquals(
            12.hours.inWholeMilliseconds,
            infos().single().periodicityInfo!!.repeatIntervalMillis,
        )
    }

    @Test
    fun `the probe waits for a network before it runs`() {
        scheduler().schedule()

        assertEquals(NetworkType.CONNECTED, infos().single().constraints.requiredNetworkType)
    }

    @Test
    fun `scheduling again keeps the window already booked`() {
        val scheduler = scheduler()
        scheduler.schedule()
        val booked = infos().single().id

        scheduler.schedule()

        assertEquals(booked, infos().single().id)
    }

    private fun scheduler() = BackgroundProbeScheduler(WorkManager.getInstance(context))

    private fun infos(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME).get()
}
