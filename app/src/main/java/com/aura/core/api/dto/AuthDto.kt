package com.aura.core.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmailCredentialsDto(
    val email: String,
    val password: String,
)

@Serializable
data class GoogleSignInRequestDto(
    @SerialName("id_token") val idToken: String,
)

@Serializable
data class PasswordResetRequestDto(
    val email: String,
)

@Serializable
data class PasswordResetConfirmDto(
    val token: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class MessageResponseDto(
    val message: String,
)

@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("is_new_account") val isNewAccount: Boolean = false,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: Int,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("auth_methods") val authMethods: List<String> = emptyList(),
    @SerialName("promo_code") val promoCode: String,
    @SerialName("invite_decision") val inviteDecision: String,
    @SerialName("gift_popup_seen") val giftPopupSeen: Boolean = false,
    @SerialName("bonus_teaser_seen") val bonusTeaserSeen: Boolean = false,
    @SerialName("push_enabled") val pushEnabled: Boolean = true,
    @SerialName("bonus_reserved_ion") val bonusReservedIon: Int = 0,
    @SerialName("accrued_ion") val accruedIon: Int = 0,
    @SerialName("withdrawable_ion") val withdrawableIon: Int = 0,
    @SerialName("spark_balance") val sparkBalance: String = "0",
    @SerialName("tap_count") val tapCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
)
