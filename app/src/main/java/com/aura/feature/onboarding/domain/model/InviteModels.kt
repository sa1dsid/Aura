package com.aura.feature.onboarding.domain.model

sealed interface InviteAttribution {
    data object None : InviteAttribution

    data class FromLink(val code: String) : InviteAttribution
}

enum class InviteFailure {
    UNKNOWN_CODE,
    OWN_CODE,
    OWNER_DELETED,
    ALREADY_APPLIED,
    NETWORK,
}

data class OnboardingFlags(
    val inviteScreenPassed: Boolean,
    val bonusPopupShown: Boolean,
    val reservedBonusIon: Int,
)

const val INVITE_CODE_LENGTH = 8
