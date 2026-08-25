package com.aura.feature.onboarding.data.repository

import com.aura.feature.onboarding.FakeOnboardingRemoteDataSource
import com.aura.feature.onboarding.data.remote.dto.BootConfigDto
import com.aura.feature.onboarding.data.remote.dto.OnboardingFlagsDto
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.model.OnboardingFlags
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

private const val ACCOUNT_ID = "309"

class OnboardingFlagsRepositoryImplTest {

    private val remote = FakeOnboardingRemoteDataSource()

    private fun repository() = OnboardingFlagsRepositoryImpl(remote, Dispatchers.Unconfined)

    private fun bootRepository() = BootRepositoryImpl(remote, Dispatchers.Unconfined)

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
        remote.flagsError = SocketTimeoutException("timeout")

        val flags = repository().flags(ACCOUNT_ID)

        assertEquals(OnboardingFlags.FALLBACK, flags)
        assertFalse(flags.bonusPopupShown)
        assertEquals(3_000L, flags.reservedBonusIon)
    }

    @Test
    fun `survives a timeout while marking the bonus popup shown`() = runTest {
        remote.markBonusPopupShownError = SocketTimeoutException("timeout")

        repository().markBonusPopupShown(ACCOUNT_ID)

        assertEquals(1, remote.markBonusPopupShownCalls)
    }

    @Test
    fun `a cancelled flags read is not swallowed as the fallback`() {
        remote.flagsError = CancellationException("gone")

        assertThrows(CancellationException::class.java) {
            runBlocking { repository().flags(ACCOUNT_ID) }
        }
    }

    @Test
    fun `the boot config carries what the server sent`() = runTest {
        remote.bootConfig = BootConfigDto(nodeCount = 12_048, hotCities = listOf("Tallinn"))

        assertEquals(
            BootConfig(nodeCount = 12_048, hotCities = listOf("Tallinn")),
            bootRepository().bootstrap(),
        )
    }

    @Test
    fun `a boot without an answer falls back to the built in node count`() = runTest {
        remote.bootstrapError = IOException("offline")

        assertEquals(
            BootConfig(nodeCount = BootConfig.DEFAULT_NODE_COUNT, hotCities = emptyList()),
            bootRepository().bootstrap(),
        )
    }

    @Test
    fun `a cancelled boot is not swallowed as the fallback`() {
        remote.bootstrapError = CancellationException("gone")

        assertThrows(CancellationException::class.java) {
            runBlocking { bootRepository().bootstrap() }
        }
    }
}
