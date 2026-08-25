package com.aura.feature.onboarding.data.mapper

import com.aura.feature.onboarding.data.remote.dto.AccountDto
import com.aura.feature.onboarding.data.remote.dto.AuthSessionDto
import com.aura.feature.onboarding.data.remote.dto.BootConfigDto
import com.aura.feature.onboarding.data.remote.dto.OnboardingFlagsDto
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.model.OnboardingFlags
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingMapperTest {

    @Test
    fun `an account keeps every field the screens read`() {
        val account = accountDto().toDomain()

        assertEquals(
            Account(
                id = "39",
                email = "said@ioaura.app",
                handle = "said",
                inviteLink = "https://ioaura.app/i/SYREX482",
                authProvider = AuthProvider.EMAIL,
            ),
            account,
        )
    }

    @Test
    fun `the invite code itself is dropped on the way to the domain`() {
        val account = accountDto(inviteCode = "SYREX482").toDomain()

        assertEquals("https://ioaura.app/i/SYREX482", account.inviteLink)
    }

    @Test
    fun `a google account is recognised`() {
        assertEquals(AuthProvider.GOOGLE, accountDto(authProvider = "GOOGLE").toDomain().authProvider)
    }

    @Test
    fun `an unknown provider falls back to email`() {
        assertEquals(AuthProvider.EMAIL, accountDto(authProvider = "apple").toDomain().authProvider)
    }

    @Test
    fun `a provider in the wrong case falls back to email`() {
        assertEquals(AuthProvider.EMAIL, accountDto(authProvider = "google").toDomain().authProvider)
    }

    @Test
    fun `a session carries the new account and pending invite marks`() {
        val session = AuthSessionDto(
            account = accountDto(),
            accountCreated = true,
            invitePending = true,
        ).toDomain()

        assertEquals(true, session.accountCreated)
        assertEquals(true, session.invitePending)
        assertEquals("39", session.account.id)
    }

    @Test
    fun `flags cross over untouched`() {
        val flags = OnboardingFlagsDto(
            inviteScreenPassed = true,
            bonusPopupShown = true,
            reservedBonusIon = 1_500L,
        ).toDomain()

        assertEquals(
            OnboardingFlags(
                inviteScreenPassed = true,
                bonusPopupShown = true,
                reservedBonusIon = 1_500L,
            ),
            flags,
        )
    }

    @Test
    fun `a boot config without a node count falls back to the built in one`() {
        val config = BootConfigDto(nodeCount = null, hotCities = listOf("Tallinn")).toDomain()

        assertEquals(BootConfig(BootConfig.DEFAULT_NODE_COUNT, listOf("Tallinn")), config)
        assertEquals(4_210, BootConfig.DEFAULT_NODE_COUNT)
    }

    @Test
    fun `a boot config keeps the node count the server sent`() {
        val config = BootConfigDto(nodeCount = 12_048, hotCities = emptyList()).toDomain()

        assertEquals(12_048, config.nodeCount)
    }

    private fun accountDto(
        inviteCode: String = "SYREX482",
        authProvider: String = "EMAIL",
    ) = AccountDto(
        id = "39",
        email = "said@ioaura.app",
        handle = "said",
        inviteCode = inviteCode,
        inviteLink = "https://ioaura.app/i/$inviteCode",
        authProvider = authProvider,
    )
}
