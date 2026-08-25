package com.aura.feature.onboarding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before

private const val POLL_MILLIS = 2L

private const val AWAIT_MILLIS = 3_000L

@OptIn(ExperimentalCoroutinesApi::class)
abstract class OnboardingTestCase {

    @Before
    fun installMainDispatcher() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun removeMainDispatcher() {
        Dispatchers.resetMain()
    }

    internal fun onboarding(
        referrerCode: String? = null,
        body: suspend CoroutineScope.(OnboardingStack) -> Unit,
    ) = runBlocking {
        val stack = OnboardingStack(referrerCode)
        try {
            body(stack)
        } finally {
            stack.close()
        }
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
