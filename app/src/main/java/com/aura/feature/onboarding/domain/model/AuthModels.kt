package com.aura.feature.onboarding.domain.model

enum class AuthMode { SIGN_IN, SIGN_UP }

data class AuthSession(
    val account: Account,
    val accountCreated: Boolean,
    val invitePending: Boolean,
)

enum class AuthFailure {
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    PASSWORD_TOO_SHORT,
    EMAIL_ALREADY_REGISTERED,
    ACCOUNT_NOT_FOUND,
    WRONG_PASSWORD,
    GOOGLE_CANCELLED,
    GOOGLE_UNAVAILABLE,
    NETWORK,
}

const val MIN_PASSWORD_LENGTH = 8
