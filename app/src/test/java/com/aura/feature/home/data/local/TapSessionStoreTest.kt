package com.aura.feature.home.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TapSessionStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun store(): TapSessionStore = DataStoreTapSessionStore(context)

    @Before
    fun emptyTheStore() = runTest {
        store().clearPendingSessionId()
        store().saveRate(0)
    }

    @Test
    fun `a store nobody wrote to reports no rate and no session`() = runTest {
        val store = store()

        assertEquals(0, store.rate())
        assertNull(store.pendingSessionId())
    }

    @Test
    fun `the window rate outlives the process`() = runTest {
        store().saveRate(40_000)

        assertEquals(40_000, store().rate())
    }

    @Test
    fun `a started session is remembered so it can be released later`() = runTest {
        store().savePendingSessionId("session-1")

        assertEquals("session-1", store().pendingSessionId())
    }

    @Test
    fun `a released session is forgotten for good`() = runTest {
        val store = store()
        store.savePendingSessionId("session-1")

        store.clearPendingSessionId()

        assertNull(store().pendingSessionId())
    }

    @Test
    fun `a second session replaces the one before it`() = runTest {
        val store = store()
        store.savePendingSessionId("session-1")

        store.savePendingSessionId("session-2")

        assertEquals("session-2", store.pendingSessionId())
    }

    @Test
    fun `clearing the session leaves the window rate alone`() = runTest {
        val store = store()
        store.saveRate(20_000)
        store.savePendingSessionId("session-1")

        store.clearPendingSessionId()

        assertEquals(20_000, store.rate())
    }
}
