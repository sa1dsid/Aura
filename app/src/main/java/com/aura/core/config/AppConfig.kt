package com.aura.core.config

import androidx.compose.runtime.Immutable

@Immutable
data class FeatureFlags(
    val dataShare: Boolean = false,
    val trafficWithdrawals: Boolean = false,
    val vpnCode: Boolean = false,
)

enum class SocialNetwork { TELEGRAM, DISCORD, X, INSTAGRAM, YOUTUBE, TIKTOK, REDDIT, SNAPCHAT }

@Immutable
data class SocialLink(
    val network: SocialNetwork,
    val url: String,
)

@Immutable
data class IoniConfig(
    val supportEmail: String = "",
    val assistantEnabled: Boolean = false,
    val aiReleased: Boolean = false,
)

@Immutable
data class AppConfig(
    val termsUrl: String = "",
    val privacyUrl: String = "",
    val featureFlags: FeatureFlags = FeatureFlags(),
    val socialLinks: List<SocialLink> = emptyList(),
    val ioni: IoniConfig = IoniConfig(),
)
