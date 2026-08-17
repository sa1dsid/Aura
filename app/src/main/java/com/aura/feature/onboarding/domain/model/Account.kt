package com.aura.feature.onboarding.domain.model

enum class AuthProvider { GOOGLE, EMAIL }

data class Account(
    val id: String,
    val email: String,
    val handle: String,
    val inviteLink: String,
    val authProvider: AuthProvider,
)
