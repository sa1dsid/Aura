package com.aura.feature.onboarding.presentation.auth

import com.aura.feature.onboarding.domain.model.AuthMode

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val invalidField: AuthField? = null,
)

enum class AuthField { EMAIL, PASSWORD }

enum class AuthToast {
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    PASSWORD_TOO_SHORT,
    WRONG_CREDENTIALS,
    ACCOUNT_EXISTS,
    NO_ACCOUNT,
    NO_CONNECTION,
    RESET_LINK_SENT,
}

sealed interface AuthEvent {
    data object OpenHome : AuthEvent

    data object OpenInvite : AuthEvent

    data class ShowToast(val toast: AuthToast) : AuthEvent
}
