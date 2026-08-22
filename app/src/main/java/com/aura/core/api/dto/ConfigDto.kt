package com.aura.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreferenceUpdateDto(
    @SerialName("push_enabled") val pushEnabled: Boolean,
)

@Serializable
data class PublicConfigDto(
    @SerialName("terms_url") val termsUrl: String = "",
    @SerialName("privacy_url") val privacyUrl: String = "",
)
