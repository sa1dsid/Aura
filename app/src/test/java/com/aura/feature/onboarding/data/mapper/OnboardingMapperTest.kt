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
    fun `the invite link crosses over as the server built it`() {
        val account = accountDto().toDomain()

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
    fun `a session carries the pending invite mark`() {
        val session = AuthSessionDto(account = accountDto(), invitePending = true).toDomain()

        assertEquals(true, session.invitePending)
        assertEquals("39", session.account.id)
    }

    @Test
    fun `flags cross over untouched`() {
        val flags = OnboardingFlagsDto(bonusPopupShown = true, reservedBonusIon = 1_500L).toDomain()

        assertEquals(OnboardingFlags(bonusPopupShown = true, reservedBonusIon = 1_500L), flags)
    }

    @Test
    fun `a boot config without a node count falls back to the built in one`() {
        assertEquals(BootConfig.FALLBACK, BootConfigDto(nodeCount = null).toDomain())
        assertEquals(4_210, BootConfig.DEFAULT_NODE_COUNT)
    }

    @Test
    fun `a boot config keeps the node count the server sent`() {
        assertEquals(12_048, BootConfigDto(nodeCount = 12_048).toDomain().nodeCount)
    }

    private fun accountDto(authProvider: String = "EMAIL") = AccountDto(
        id = "39",
        email = "said@ioaura.app",
        handle = "said",
        inviteLink = "https://ioaura.app/i/SYREX482",
        authProvider = authProvider,
    )
}
