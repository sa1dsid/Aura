package com.aura.feature.onboarding.data.remote.dto

data class AccountDto(
    val id: String,
    val email: String,
    val handle: String,
    val inviteLink: String,
    val authProvider: String,
)

data class AuthSessionDto(
    val account: AccountDto,
    val invitePending: Boolean,
)

data class EmailVerificationDto(
    val email: String,
    val expiresInSeconds: Int,
)

data class OnboardingFlagsDto(
    val bonusPopupShown: Boolean,
    val reservedBonusIon: Long,
)

data class BootConfigDto(val nodeCount: Int?)
