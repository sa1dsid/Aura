package com.aura.feature.onboarding.data.attribution

import com.aura.feature.onboarding.domain.model.InviteAttribution
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val CODE = "SYREX482"

class InviteAttributionStoreTest {

    @Test
    fun `an install without a link carries no code`() = runTest {
        val store = store(referrer = null)

        assertEquals(InviteAttribution.None, store.pending())
    }

    @Test
    fun `the install referrer carries the code of the first launch`() = runTest {
        val store = store(referrer = CODE)

        assertEquals(InviteAttribution.FromLink(CODE), store.pending())
    }

    @Test
    fun `a deep link opened later wins over the install referrer`() = runTest {
        val store = store(referrer = "AAAA1111")
        store.pending()

        store.rememberDeepLink("BBBB2222")

        assertEquals(InviteAttribution.FromLink("BBBB2222"), store.pending())
    }

    @Test
    fun `the install referrer is read once no matter how often the screen asks`() = runTest {
        val source = FakeReferrerSource(CODE)
        val store = InviteAttributionStore(source)

        repeat(5) { store.pending() }

        assertEquals(1, source.reads)
    }

    @Test
    fun `a used up code is gone for good`() = runTest {
        val store = store(referrer = CODE)
        store.pending()

        store.consume()

        assertEquals(InviteAttribution.None, store.pending())
    }

    @Test
    fun `a link opened after the decision is ignored`() = runTest {
        val store = store(referrer = null)
        store.consume()

        store.rememberDeepLink(CODE)

        assertEquals(InviteAttribution.None, store.pending())
    }

    @Test
    fun `the install referrer is not read after the decision`() = runTest {
        val source = FakeReferrerSource(CODE)
        val store = InviteAttributionStore(source)

        store.consume()
        store.pending()

        assertEquals(0, source.reads)
    }

    @Test
    fun `a lower case code is stored upper cased`() = runTest {
        val store = store(referrer = "syrex482")

        assertEquals(InviteAttribution.FromLink(CODE), store.pending())
    }

    @Test
    fun `spaces and punctuation around a code are dropped`() = runTest {
        val store = store(referrer = null)

        store.rememberDeepLink("  syrex-482 ")

        assertEquals(InviteAttribution.FromLink(CODE), store.pending())
    }

    @Test
    fun `a code that is not eight characters long is refused`() = runTest {
        val store = store(referrer = null)

        store.rememberDeepLink("SYREX48")
        store.rememberDeepLink("SYREX4821")
        store.rememberDeepLink("")

        assertEquals(InviteAttribution.None, store.pending())
    }

    @Test
    fun `a malformed link does not erase the code already remembered`() = runTest {
        val store = store(referrer = null)
        store.rememberDeepLink(CODE)

        store.rememberDeepLink("nope")

        assertEquals(InviteAttribution.FromLink(CODE), store.pending())
    }

    @Test
    fun `a referrer code of the wrong length leaves the screen empty`() = runTest {
        val store = store(referrer = "TOOLONGCODE")

        assertEquals(InviteAttribution.None, store.pending())
    }

    private fun store(referrer: String?) = InviteAttributionStore(FakeReferrerSource(referrer))

    private class FakeReferrerSource(private val code: String?) : InstallReferrerSource {
        var reads = 0

        override suspend fun inviteCode(): String? {
            reads++
            return code
        }
    }
}
