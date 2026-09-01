package com.aura.core.api.dto

import com.aura.core.api.serialization.DecimalAsLongSerializer
import com.aura.core.api.serialization.DecimalAsStringSerializer
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
data class EmailVerificationConfirmDto(
    val email: String,
    val code: String,
)

@Serializable
data class EmailVerificationResendDto(
    val email: String,
)

@Serializable
data class EmailVerificationPendingDto(
    val message: String,
    val email: String,
    @SerialName("expires_in") val expiresIn: Int,
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
    @Serializable(DecimalAsLongSerializer::class)
    @SerialName("bonus_reserved_ion") val bonusReservedIon: Long = 0,
    @Serializable(DecimalAsLongSerializer::class)
    @SerialName("accrued_ion") val accruedIon: Long = 0,
    @Serializable(DecimalAsLongSerializer::class)
    @SerialName("withdrawable_ion") val withdrawableIon: Long = 0,
    @Serializable(DecimalAsStringSerializer::class)
    @SerialName("spark_balance") val sparkBalance: String = "0",
    @SerialName("tap_count") val tapCount: Int = 0,
    val country: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)
