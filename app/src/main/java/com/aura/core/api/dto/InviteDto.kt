package com.aura.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteApplyDto(
    val code: String,
    val source: String = "manual",
)

@Serializable
data class InviteStateDto(
    val decision: String,
    @SerialName("applied_code") val appliedCode: String? = null,
    val source: String? = null,
    @SerialName("personal_code") val personalCode: String,
    @SerialName("personal_url") val personalUrl: String,
    @SerialName("share_text") val shareText: String,
)

@Serializable
data class GiftPopupSeenDto(
    @SerialName("gift_popup_seen") val giftPopupSeen: Boolean = true,
    @SerialName("bonus_teaser_should_blink") val bonusTeaserShouldBlink: Boolean = false,
)
