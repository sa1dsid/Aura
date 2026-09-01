package com.aura.feature.onboarding.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class AuthMode { SIGN_IN, SIGN_UP }

data class AuthSession(
    val account: Account,
    val invitePending: Boolean,
)

enum class AuthFailure {
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    EMAIL_ALREADY_REGISTERED,
    ACCOUNT_NOT_FOUND,
    WRONG_PASSWORD,
    EMAIL_NOT_VERIFIED,
    GOOGLE_CANCELLED,
    GOOGLE_UNAVAILABLE,
    NETWORK,
}

const val MIN_PASSWORD_LENGTH = 8

data class EmailVerification(
    val email: String,
    val codeLifetime: Duration = DEFAULT_CODE_LIFETIME,
) {
    companion object {
        val DEFAULT_CODE_LIFETIME = 10.minutes

        fun of(email: String, expiresInSeconds: Int) = EmailVerification(
            email = email,
            codeLifetime = expiresInSeconds.takeIf { it > 0 }?.seconds ?: DEFAULT_CODE_LIFETIME,
        )
    }
}

enum class EmailVerificationFailure {
    CODE_REJECTED,
    RESEND_TOO_SOON,
    NETWORK,
}

const val EMAIL_CODE_LENGTH = 6

fun String.toEmailCode(): String = filter(Char::isDigit).take(EMAIL_CODE_LENGTH)

val String.isWholeEmailCode: Boolean
    get() = length == EMAIL_CODE_LENGTH
