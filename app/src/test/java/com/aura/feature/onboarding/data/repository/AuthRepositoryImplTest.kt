package com.aura.feature.onboarding.data.repository

import com.aura.feature.onboarding.FakeOnboardingRemoteDataSource
import com.aura.feature.onboarding.FakePushTokenRepository
import com.aura.feature.onboarding.FakeTokenStore
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.onboarding.testAccount
import com.aura.feature.onboarding.testSessionDto
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.StartDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

private const val TOKEN = "header.payload.signature"

class AuthRepositoryImplTest {

    private val remote = FakeOnboardingRemoteDataSource()

    private val sessionStore = SessionStore()

    private val tokenStore = FakeTokenStore()

    private val push = FakePushTokenRepository()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private fun repository() = AuthRepositoryImpl(
        remote = remote,
        sessionStore = sessionStore,
        tokenStore = tokenStore.store,
        pushTokenRepository = push.repository,
        applicationScope = applicationScope,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `without a token the app opens auth and never asks the server`() = runTest {
        assertEquals(StartDestination.AUTH, repository().restoreSession())
        assertEquals(0, remote.restoreCalls)
        assertNull(sessionStore.account.value)
    }

    @Test
    fun `a restored session with a pending invite opens the invite screen`() = runTest {
        tokenStore.store.save(TOKEN, 604_800)
        remote.session = testSessionDto(invitePending = true)

        assertEquals(StartDestination.INVITE, repository().restoreSession())
        assertEquals("39", sessionStore.account.value?.id)
        assertEquals(1, push.refreshes)
    }

    @Test
    fun `a restored session with a settled invite opens home`() = runTest {
        tokenStore.store.save(TOKEN, 604_800)
        remote.session = testSessionDto(invitePending = false)

        assertEquals(StartDestination.HOME, repository().restoreSession())
    }

    @Test
    fun `a rejected token is thrown away and the app opens auth`() = runTest {
        tokenStore.store.save(TOKEN, 604_800)
        remote.restoreError = httpError(401)

        assertEquals(StartDestination.AUTH, repository().restoreSession())
        assertNull(tokenStore.token)
        assertEquals(1, tokenStore.clears)
        assertNull(sessionStore.account.value)
    }

    @Test
    fun `any other server refusal keeps the token but never opens home`() = runTest {
        tokenStore.store.save(TOKEN, 604_800)
        remote.restoreError = httpError(403)

        assertEquals(StartDestination.AUTH, repository().restoreSession())
        assertEquals(TOKEN, tokenStore.token)
        assertEquals(0, tokenStore.clears)
    }

    @Test
    fun `a dead network keeps the token but never opens home`() = runTest {
        tokenStore.store.save(TOKEN, 604_800)
        remote.restoreError = IOException("offline")

        assertEquals(StartDestination.AUTH, repository().restoreSession())
        assertEquals(TOKEN, tokenStore.token)
        assertNull(sessionStore.account.value)
    }

    @Test
    fun `a cancelled restore is not swallowed as a home destination`() {
        runBlocking { tokenStore.store.save(TOKEN, 604_800) }
        remote.restoreError = CancellationException("gone")

        assertThrows(CancellationException::class.java) {
            runBlocking { repository().restoreSession() }
        }
    }

    @Test
    fun `signing in opens the session and refreshes the push subscription`() = runTest {
        remote.session = testSessionDto(invitePending = true)

        val result = repository().signIn("said@ioaura.app", "Password123")

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull()?.invitePending)
        assertEquals("said", sessionStore.account.value?.handle)
        assertEquals(1, push.refreshes)
    }

    @Test
    fun `a refused sign in leaves no session behind`() = runTest {
        remote.signInError = httpError(401, """{"detail":"Wrong email or password"}""")

        val result = repository().signIn("said@ioaura.app", "Password123")

        assertEquals(AuthFailure.WRONG_PASSWORD, result.failure())
        assertNull(sessionStore.account.value)
        assertEquals(0, push.refreshes)
    }

    @Test
    fun `a dropped sign in reads as a network failure`() = runTest {
        remote.signInError = IOException("offline")

        assertEquals(
            AuthFailure.NETWORK,
            repository().signIn("said@ioaura.app", "Password123").failure(),
        )
    }

    @Test
    fun `a taken email is reported from sign up`() = runTest {
        remote.signUpError = httpError(409, """{"detail":"Account already exists"}""")

        assertEquals(
            AuthFailure.EMAIL_ALREADY_REGISTERED,
            repository().signUp("said@ioaura.app", "Password123").failure(),
        )
    }

    @Test
    fun `a cancelled sign in is not swallowed as a failed result`() {
        remote.signInError = CancellationException("gone")

        assertThrows(CancellationException::class.java) {
            runBlocking { repository().signIn("said@ioaura.app", "Password123") }
        }
    }

    @Test
    fun `google sends the token it was given`() = runTest {
        repository().continueWithGoogle("google.id.token")

        assertEquals(listOf("google.id.token"), remote.sentIdTokens)
    }

    @Test
    fun `a rejected google token is never reported as a wrong password`() = runTest {
        remote.googleErrors += httpError(401, """{"detail":"Invalid Google token"}""")

        assertEquals(
            AuthFailure.GOOGLE_UNAVAILABLE,
            repository().continueWithGoogle("google.id.token").failure(),
        )
    }

    @Test
    fun `google tries once more when the connection breaks`() = runTest {
        remote.googleErrors += IOException("broken pipe")

        val result = repository().continueWithGoogle("google.id.token")

        assertTrue(result.isSuccess)
        assertEquals(2, remote.googleCalls)
    }

    @Test
    fun `google gives up after the second broken connection`() = runTest {
        remote.googleErrors += IOException("broken pipe")
        remote.googleErrors += IOException("broken pipe")

        val result = repository().continueWithGoogle("google.id.token")

        assertEquals(AuthFailure.NETWORK, result.failure())
        assertEquals(2, remote.googleCalls)
    }

    @Test
    fun `google does not try again when the server stays silent`() = runTest {
        remote.googleErrors += SocketTimeoutException("timeout")

        val result = repository().continueWithGoogle("google.id.token")

        assertEquals(AuthFailure.NETWORK, result.failure())
        assertEquals(1, remote.googleCalls)
    }

    @Test
    fun `email sign in is never retried on a broken connection`() = runTest {
        remote.signInError = IOException("broken pipe")

        repository().signIn("said@ioaura.app", "Password123")

        assertEquals(1, remote.signInCalls)
        assertEquals(0, push.refreshes)
    }

    @Test
    fun `a password reset carries the email through`() = runTest {
        val result = repository().requestPasswordReset("said@ioaura.app")

        assertTrue(result.isSuccess)
        assertEquals(listOf("said@ioaura.app"), remote.resetEmails)
    }

    @Test
    fun `a refused password reset reads as a network failure`() = runTest {
        remote.resetError = httpError(500)

        assertEquals(
            AuthFailure.NETWORK,
            repository().requestPasswordReset("said@ioaura.app").failure(),
        )
    }

    @Test
    fun `the current account is whatever the open session holds`() = runTest {
        val repository = repository()

        assertNull(repository.currentAccount())

        sessionStore.open(testAccount("7"))

        assertEquals("7", repository.currentAccount()?.id)

        sessionStore.close()

        assertNull(repository.currentAccount())
    }

    private fun Result<*>.failure(): AuthFailure? =
        (exceptionOrNull() as? AuthException)?.failure

    private fun httpError(code: Int, body: String = "{}") = HttpException(
        Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType()))
    )
}
