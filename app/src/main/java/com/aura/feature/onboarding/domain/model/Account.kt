package com.aura.feature.onboarding.domain.model

data class Account(
    val id: String,
    val email: String,
    val handle: String,
    val inviteLink: String,
)
