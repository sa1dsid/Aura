package com.aura.feature.account.domain.model

import com.aura.feature.onboarding.domain.model.AuthProvider

data class AccountProfile(
    val handle: String,
    val email: String,
    val authProvider: AuthProvider,
)

data class LegalLinks(
    val termsUrl: String,
    val privacyUrl: String,
) {
    companion object {
        val EMPTY = LegalLinks(termsUrl = "", privacyUrl = "")
    }
}
