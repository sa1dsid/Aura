package com.aura.core.auth

import android.content.Context
import android.os.Bundle
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PrepareGetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.aura.core.network.NetworkMonitor
import com.aura.core.network.NetworkStatus
import com.aura.feature.onboarding.domain.model.AuthException
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val ID_TOKEN = "google.id.token"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GoogleSignInClientTest {

    private val credentialManager: CredentialManager = mockk()

    private val networkMonitor = FakeNetworkMonitor()

    private val activityContext: Context = mockk(relaxed = true)

    private val client = GoogleSignInClient(credentialManager, networkMonitor)

    @Test
    fun `the id token of the picked account is handed over`() = runTest {
        answerWith(googleCredential())

        assertEquals(ID_TOKEN, client.idToken(activityContext))
    }

    @Test
    fun `an account already known to the app is asked for first`() = runTest {
        answerWith(googleCredential())

        client.idToken(activityContext)

        coVerify(exactly = 1) { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) }
    }

    @Test
    fun `closing the sheet is told apart from an outage`() = runTest {
        coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } throws
            GetCredentialCancellationException()

        assertEquals(AuthFailure.GOOGLE_CANCELLED, failureOf { client.idToken(activityContext) })
    }

    @Test
    fun `an account picker with nothing to offer is asked three times in all`() = runTest {
        coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } throws
            NoCredentialException()

        assertEquals(AuthFailure.GOOGLE_UNAVAILABLE, failureOf { client.idToken(activityContext) })
        coVerify(exactly = 3) {
            credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
        }
    }

    @Test
    fun `an empty picker on a dead network reads as a connection problem`() = runTest {
        networkMonitor.set(NetworkStatus.Offline)
        coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } throws
            NoCredentialException()

        assertEquals(AuthFailure.NETWORK, failureOf { client.idToken(activityContext) })
    }

    @Test
    fun `a credential manager that will not talk reads as google unavailable`() = runTest {
        coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } throws
            GetCredentialUnknownException()

        assertEquals(AuthFailure.GOOGLE_UNAVAILABLE, failureOf { client.idToken(activityContext) })
    }

    @Test
    fun `a credential that is not a google one is refused`() = runTest {
        answerWith(CustomCredential("com.example.other", Bundle()))

        assertEquals(AuthFailure.GOOGLE_UNAVAILABLE, failureOf { client.idToken(activityContext) })
    }

    @Test
    fun `the picker falls back to every account when none is authorised yet`() = runTest {
        var call = 0
        coEvery {
            credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
        } answers {
            call++
            if (call == 1) throw NoCredentialException()
            GetCredentialResponse(googleCredential())
        }

        assertEquals(ID_TOKEN, client.idToken(activityContext))
        assertEquals(2, call)
    }

    @Test
    fun `warming up stops at the first answer`() = runTest {
        coEvery {
            credentialManager.prepareGetCredential(any())
        } returns mockk<PrepareGetCredentialResponse>()

        client.warmUp()

        coVerify(exactly = 1) { credentialManager.prepareGetCredential(any()) }
    }

    @Test
    fun `warming up gives the play services three chances`() = runTest {
        coEvery { credentialManager.prepareGetCredential(any()) } throws IllegalStateException("cold")

        client.warmUp()

        coVerify(exactly = 3) { credentialManager.prepareGetCredential(any()) }
    }

    private fun answerWith(credential: androidx.credentials.Credential) {
        coEvery {
            credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
        } returns GetCredentialResponse(credential)
    }

    private fun googleCredential() = GoogleIdTokenCredential.Builder()
        .setIdToken(ID_TOKEN)
        .setId("said@ioaura.app")
        .build()

    private suspend fun failureOf(block: suspend () -> Unit): AuthFailure? =
        (runCatching { block() }.exceptionOrNull() as? AuthException)?.failure

    private class FakeNetworkMonitor : NetworkMonitor {
        private val state = MutableStateFlow(
            NetworkStatus(isOnline = true, isVpnActive = false)
        )

        override val status: StateFlow<NetworkStatus> = state

        override fun current(): NetworkStatus = state.value

        fun set(value: NetworkStatus) {
            state.value = value
        }
    }
}
