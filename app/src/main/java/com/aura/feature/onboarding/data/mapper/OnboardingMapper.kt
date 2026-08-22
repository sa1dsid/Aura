package com.aura.feature.onboarding.data.mapper

import com.aura.feature.onboarding.data.remote.dto.AccountDto
import com.aura.feature.onboarding.data.remote.dto.AuthSessionDto
import com.aura.feature.onboarding.data.remote.dto.BootConfigDto
import com.aura.feature.onboarding.data.remote.dto.OnboardingFlagsDto
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.model.AuthSession
import com.aura.feature.onboarding.domain.model.BootConfig
import com.aura.feature.onboarding.domain.model.OnboardingFlags

fun AccountDto.toDomain() = Account(
    id = id,
    email = email,
    handle = handle,
    inviteLink = inviteLink,
    authProvider = authProvider.toAuthProvider(),
)

fun AuthSessionDto.toDomain() = AuthSession(
    account = account.toDomain(),
    accountCreated = accountCreated,
    invitePending = invitePending,
)

fun OnboardingFlagsDto.toDomain() = OnboardingFlags(
    inviteScreenPassed = inviteScreenPassed,
    bonusPopupShown = bonusPopupShown,
    reservedBonusIon = reservedBonusIon,
)

fun BootConfigDto.toDomain() = BootConfig(
    nodeCount = nodeCount ?: BootConfig.DEFAULT_NODE_COUNT,
    hotCities = hotCities,
)

private fun String.toAuthProvider(): AuthProvider =
    AuthProvider.entries.firstOrNull { it.name == this } ?: AuthProvider.EMAIL
