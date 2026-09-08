package com.aura.core.crash

import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrashUserTrackerTest {

    @Test
    fun `the signed in account reaches the reporter`() = runTest {
        val reporter = RecordingCrashReporter()
        val sessionStore = SessionStore()
        tracker(sessionStore, reporter, backgroundScope)
        runCurrent()

        sessionStore.open(accountOf("39"))
        runCurrent()

        assertEquals(listOf(null, "39"), reporter.users)
    }

    @Test
    fun `signing out drops the account from crash reports`() = runTest {
        val reporter = RecordingCrashReporter()
        val sessionStore = SessionStore().apply { open(accountOf("39")) }
        tracker(sessionStore, reporter, backgroundScope)
        runCurrent()

        sessionStore.close()
        runCurrent()

        assertEquals(listOf("39", null), reporter.users)
    }

    @Test
    fun `the same account is reported once`() = runTest {
        val reporter = RecordingCrashReporter()
        val sessionStore = SessionStore()
        tracker(sessionStore, reporter, backgroundScope)
        runCurrent()

        sessionStore.open(accountOf("39"))
        sessionStore.open(accountOf("39", handle = "renamed"))
        runCurrent()

        assertEquals(listOf(null, "39"), reporter.users)
    }

    private fun TestScope.tracker(
        sessionStore: SessionStore,
        reporter: CrashReporter,
        scope: CoroutineScope,
    ) = CrashUserTracker(sessionStore, reporter, scope).also(CrashUserTracker::track)

    private fun accountOf(id: String, handle: String = "syrex") = Account(
        id = id,
        email = "smoke@auratest.dev",
        handle = handle,
        inviteLink = "https://ioaura.app/i/$handle",
        authProvider = AuthProvider.EMAIL,
    )
}

private class RecordingCrashReporter : CrashReporter {

    val users = mutableListOf<String?>()

    override fun setUser(id: String?) {
        users += id
    }

    override fun log(message: String) = Unit

    override fun record(error: Throwable) = Unit
}
