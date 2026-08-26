package com.aura.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before

private const val POLL_MILLIS = 2L

private const val AWAIT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
abstract class IntegrationTestCase {

    @Before
    fun installMainDispatcher() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun removeMainDispatcher() {
        Dispatchers.resetMain()
    }

    protected fun awaitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + AWAIT_MILLIS
        while (!condition() && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MILLIS)
        }
        if (!condition()) fail("timed out waiting for $what")
    }

    protected fun awaitEvent(events: List<*>, count: Int = 1) =
        awaitUntil("$count event(s)") { events.size >= count }
}
