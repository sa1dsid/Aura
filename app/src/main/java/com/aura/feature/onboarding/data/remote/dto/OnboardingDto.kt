package com.aura.feature.onboarding.data.remote.dto

data class AccountDto(
    val id: String,
    val email: String,
    val handle: String,
    val inviteCode: String,
    val inviteLink: String,
    val authProvider: String,
)

data class AuthSessionDto(
    val account: AccountDto,
    val accountCreated: Boolean,
)

data class OnboardingFlagsDto(
    val inviteScreenPassed: Boolean,
    val bonusPopupShown: Boolean,
    val reservedBonusIon: Int,
)

data class BootConfigDto(
    val nodeCount: Int?,
    val hotCities: List<String>,
)
