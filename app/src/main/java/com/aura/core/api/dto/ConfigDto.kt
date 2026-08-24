package com.aura.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreferenceUpdateDto(
    @SerialName("push_enabled") val pushEnabled: Boolean,
)

@Serializable
data class FeatureFlagsDto(
    @SerialName("data_share") val dataShare: Boolean = false,
    @SerialName("traffic_withdrawals") val trafficWithdrawals: Boolean = false,
    @SerialName("vpn_code") val vpnCode: Boolean = false,
)

@Serializable
data class SocialLinkDto(
    val kind: String = "",
    val url: String = "",
)

@Serializable
data class PublicConfigDto(
    @SerialName("terms_url") val termsUrl: String = "",
    @SerialName("privacy_url") val privacyUrl: String = "",
    @SerialName("feature_flags") val featureFlags: FeatureFlagsDto = FeatureFlagsDto(),
    @SerialName("social_links") val socialLinks: List<SocialLinkDto> = emptyList(),
)
