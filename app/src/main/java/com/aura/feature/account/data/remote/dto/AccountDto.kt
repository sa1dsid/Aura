package com.aura.feature.account.data.remote.dto

data class AccountSettingsDto(
    val pushNotifications: Boolean,
)

data class LegalLinksDto(
    val termsUrl: String,
    val privacyUrl: String,
)
