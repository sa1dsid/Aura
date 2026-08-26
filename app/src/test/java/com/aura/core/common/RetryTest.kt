package com.aura.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RetryTest {

    @Test
    fun `a call that works is never repeated`() = runTest {
        var calls = 0

        val result = runCatchingRetried {
            calls++
            "ok"
        }

        assertEquals(1, calls)
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun `a stalled call is given one more chance`() = runTest {
        var calls = 0

        val result = runCatchingRetried {
            calls++
            if (calls == 1) throw IOException("timeout") else "ok"
        }

        assertEquals(2, calls)
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun `the last failure is the one reported`() = runTest {
        var calls = 0

        val result = runCatchingRetried<String> {
            calls++
            throw IOException("attempt $calls")
        }

        assertEquals(2, calls)
        assertEquals("attempt 2", result.exceptionOrNull()?.message)
    }

    @Test
    fun `more attempts can be asked for`() = runTest {
        var calls = 0

        runCatchingRetried<String>(attempts = 4) {
            calls++
            throw IOException("offline")
        }

        assertEquals(4, calls)
    }

    @Test
    fun `cancellation stops everything at once`() = runTest {
        var calls = 0

        val thrown = runCatching {
            runCatchingRetried<String> {
                calls++
                throw CancellationException("stop")
            }
        }.exceptionOrNull()

        assertEquals(1, calls)
        assertTrue(thrown is CancellationException)
    }
}
