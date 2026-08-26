package com.aura.feature.onboarding.data.repository

import com.aura.feature.onboarding.FakeInviteAttributionStorage
import com.aura.feature.onboarding.FakeOnboardingRemoteDataSource
import com.aura.feature.onboarding.data.attribution.InstallReferrerSource
import com.aura.feature.onboarding.data.attribution.InviteAttributionStore
import com.aura.feature.onboarding.domain.model.InviteAttribution
import com.aura.feature.onboarding.domain.model.InviteException
import com.aura.feature.onboarding.domain.model.InviteFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private const val CODE = "SYREX482"

class InviteRepositoryImplTest {

    private val remote = FakeOnboardingRemoteDataSource()

    private val attributionStore = InviteAttributionStore(
        object : InstallReferrerSource {
            override suspend fun inviteCode(): String = CODE
        },
        FakeInviteAttributionStorage(),
    )

    private fun repository() =
        InviteRepositoryImpl(remote, attributionStore, Dispatchers.Unconfined)

    @Test
    fun `an applied code releases the attribution`() = runTest {
        val repository = repository()
        assertEquals(InviteAttribution.FromLink(CODE), repository.pendingAttribution())

        assertTrue(repository.applyCode(CODE).isSuccess)

        assertEquals(listOf(CODE), remote.appliedCodes)
        assertEquals(InviteAttribution.None, repository.pendingAttribution())
    }

    @Test
    fun `a refused code keeps the attribution for the next attempt`() = runTest {
        remote.applyError = httpError(404, """{"detail":"Invite code not found"}""")
        val repository = repository()

        val result = repository.applyCode(CODE)

        assertEquals(InviteFailure.UNKNOWN_CODE, result.failure())
        assertEquals(InviteAttribution.FromLink(CODE), repository.pendingAttribution())
    }

    @Test
    fun `a skipped invite releases the attribution`() = runTest {
        val repository = repository()

        assertTrue(repository.skipInvite().isSuccess)

        assertEquals(1, remote.skipCalls)
        assertEquals(InviteAttribution.None, repository.pendingAttribution())
    }

    @Test
    fun `a skip the server refused keeps the attribution`() = runTest {
        remote.skipError = httpError(500)
        val repository = repository()

        val result = repository.skipInvite()

        assertEquals(InviteFailure.NETWORK, result.failure())
        assertEquals(InviteAttribution.FromLink(CODE), repository.pendingAttribution())
    }

    @Test
    fun `a code opened from a link reaches the attribution store`() = runTest {
        val repository = repository()

        repository.rememberDeepLinkCode("bbbb2222")

        assertEquals(InviteAttribution.FromLink("BBBB2222"), repository.pendingAttribution())
    }

    @Test
    fun `the ways the server can refuse a code`() = runTest {
        assertEquals(InviteFailure.UNKNOWN_CODE, failureFor(httpError(404)))
        assertEquals(InviteFailure.ALREADY_APPLIED, failureFor(httpError(409)))
        assertEquals(InviteFailure.UNKNOWN_CODE, failureFor(httpError(400)))
        assertEquals(InviteFailure.NETWORK, failureFor(httpError(500)))
        assertEquals(InviteFailure.NETWORK, failureFor(IOException("offline")))
        assertEquals(
            InviteFailure.OWN_CODE,
            failureFor(httpError(422, """{"detail":"Own invite code is not allowed"}""")),
        )
        assertEquals(
            InviteFailure.UNKNOWN_CODE,
            failureFor(
                httpError(
                    422,
                    """{"detail":[{"type":"value_error","loc":["body","code"],"msg":"bad"}]}""",
                )
            ),
        )
    }

    @Test
    fun `a cancelled apply is not swallowed as a failed result`() {
        remote.applyError = CancellationException("gone")

        assertThrows(CancellationException::class.java) {
            runBlocking { repository().applyCode(CODE) }
        }
    }

    private suspend fun failureFor(error: Throwable): InviteFailure? {
        val source = FakeOnboardingRemoteDataSource().apply { applyError = error }
        val repository = InviteRepositoryImpl(
            source,
            InviteAttributionStore(
                object : InstallReferrerSource {
                    override suspend fun inviteCode(): String? = null
                },
                FakeInviteAttributionStorage(),
            ),
            Dispatchers.Unconfined,
        )
        return repository.applyCode(CODE).failure()
    }

    private fun Result<*>.failure(): InviteFailure? =
        (exceptionOrNull() as? InviteException)?.failure

    private fun httpError(code: Int, body: String = "{}") = HttpException(
        Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType()))
    )
}
