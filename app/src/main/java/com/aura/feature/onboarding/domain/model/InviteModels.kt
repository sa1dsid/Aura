package com.aura.feature.onboarding.domain.model

sealed interface InviteAttribution {
    data object None : InviteAttribution

    data class FromLink(val code: String) : InviteAttribution
}

enum class InviteFailure {
    UNKNOWN_CODE,
    OWN_CODE,
    ALREADY_APPLIED,
    NETWORK,
}

data class OnboardingFlags(
    val bonusPopupShown: Boolean,
    val reservedBonusIon: Long,
) {
    companion object {
        const val DEFAULT_RESERVED_BONUS_ION = 3_000L

        val FALLBACK = OnboardingFlags(
            bonusPopupShown = false,
            reservedBonusIon = DEFAULT_RESERVED_BONUS_ION,
        )
    }
}

const val INVITE_CODE_LENGTH = 8

fun String.toInviteCode(): String = trim()
    .uppercase()
    .filter(Char::isLetterOrDigit)
    .take(INVITE_CODE_LENGTH)

val String.isWholeInviteCode: Boolean
    get() = length == INVITE_CODE_LENGTH
