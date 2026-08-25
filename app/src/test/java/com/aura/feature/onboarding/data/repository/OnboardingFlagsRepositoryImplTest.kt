package com.aura.feature.onboarding.data.repository

import com.aura.feature.onboarding.data.remote.OnboardingRemoteDataSource
import com.aura.feature.onboarding.data.remote.dto.AuthSessionDto
import com.aura.feature.onboarding.data.remote.dto.BootConfigDto
import com.aura.feature.onboarding.data.remote.dto.OnboardingFlagsDto
import com.aura.feature.onboarding.domain.model.OnboardingFlags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.SocketTimeoutException

private const val ACCOUNT_ID = "309"

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingFlagsRepositoryImplTest {

    private val remote = FakeOnboardingRemoteDataSource()

    private fun repository() = OnboardingFlagsRepositoryImpl(
        remote = remote,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `reads the flags the server sent`() = runTest {
        remote.flags = OnboardingFlagsDto(
            inviteScreenPassed = true,
            bonusPopupShown = true,
            reservedBonusIon = 1_500L,
        )

        assertEquals(
            OnboardingFlags(
                inviteScreenPassed = true,
                bonusPopupShown = true,
                reservedBonusIon = 1_500L,
            ),
            repository().flags(ACCOUNT_ID),
        )
    }

    @Test
    fun `falls back to a pending bonus popup when the server times out`() = runTest {
        remote.failFlags = true

        val flags = repository().flags(ACCOUNT_ID)

        assertEquals(OnboardingFlags.FALLBACK, flags)
        assertFalse(flags.bonusPopupShown)
    }

    @Test
    fun `survives a timeout while marking the bonus popup shown`() = runTest {
        remote.failMarkBonusPopupShown = true

        repository().markBonusPopupShown(ACCOUNT_ID)

        assertEquals(1, remote.markBonusPopupShownCalls)
    }

    private class FakeOnboardingRemoteDataSource : OnboardingRemoteDataSource {
        var failFlags = false
        var failMarkBonusPopupShown = false
        var markBonusPopupShownCalls = 0
        var flags = OnboardingFlagsDto(
            inviteScreenPassed = false,
            bonusPopupShown = false,
            reservedBonusIon = 3_000L,
        )

        override suspend fun flags(accountId: String): OnboardingFlagsDto {
            if (failFlags) throw SocketTimeoutException("timeout")
            return flags
        }

        override suspend fun markBonusPopupShown(accountId: String) {
            markBonusPopupShownCalls++
            if (failMarkBonusPopupShown) throw SocketTimeoutException("timeout")
        }

        override suspend fun bootstrap(): BootConfigDto = throw UnsupportedOperationException()

        override suspend fun signIn(email: String, password: String): AuthSessionDto =
            throw UnsupportedOperationException()

        override suspend fun signUp(email: String, password: String): AuthSessionDto =
            throw UnsupportedOperationException()

        override suspend fun signInWithGoogle(idToken: String): AuthSessionDto =
            throw UnsupportedOperationException()

        override suspend fun restore(): AuthSessionDto = throw UnsupportedOperationException()

        override suspend fun requestPasswordReset(email: String) =
            throw UnsupportedOperationException()

        override suspend fun applyInviteCode(accountId: String, code: String) =
            throw UnsupportedOperationException()

        override suspend fun skipInvite(accountId: String) = throw UnsupportedOperationException()
    }
}
