package com.aura.core.api.dto

import com.aura.core.api.serialization.DecimalAsLongSerializer
import com.aura.core.api.serialization.DecimalAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DashboardDto(
    @Serializable(DecimalAsLongSerializer::class)
    @SerialName("accrued_ion") val accruedIon: Long = 0,
    @Serializable(DecimalAsLongSerializer::class)
    @SerialName("available_to_withdraw_ion") val availableToWithdrawIon: Long = 0,
    @Serializable(DecimalAsLongSerializer::class)
    @SerialName("reserved_bonus_ion") val reservedBonusIon: Long = 0,
    @Serializable(DecimalAsStringSerializer::class)
    @SerialName("spark_balance") val sparkBalance: String = "0",
    @SerialName("spark_coupon_campaign") val sparkCoupon: SparkCouponDto = SparkCouponDto(),
    @SerialName("tap_count") val tapCount: Int = 0,
    @SerialName("cooldown_available_at") val cooldownAvailableAt: String? = null,
    val node: NodeStatusDto = NodeStatusDto(),
    val bonus: BonusProgressDto = BonusProgressDto(),
    @SerialName("gift_popup_should_show") val giftPopupShouldShow: Boolean = false,
    @SerialName("bonus_teaser_should_blink") val bonusTeaserShouldBlink: Boolean = false,
    @SerialName("unread_news") val unreadNews: Int = 0,
    @SerialName("battery_optimization") val batteryOptimization: BatteryOptimizationDto =
        BatteryOptimizationDto(),
    @SerialName("ioni_state") val ioniState: String = "coming",
    @SerialName("ioni_last_completed_tap") val ioniLastCompletedTap: String? = null,
)

@Serializable
data class SparkCouponDto(
    val issued: Int = 0,
    val limit: Int = 0,
    val completed: Boolean = false,
    @SerialName("ready_code") val readyCode: String? = null,
)

@Serializable
data class NodeStatusDto(
    val tier: String = "idle",
    @SerialName("progress_metric") val progressMetric: String = "own_taps",
    @SerialName("progress_current") val progressCurrent: Int = 0,
    @SerialName("progress_target") val progressTarget: Int? = null,
    @SerialName("rate_multiplier_percent") val rateMultiplierPercent: Int = 100,
    @SerialName("spark_referral_percent") val sparkReferralPercent: Int = 0,
    @SerialName("ion_referral_percent_stage_2") val ionReferralPercentStage2: Int = 0,
    @SerialName("active_friends") val activeFriends: Int? = null,
)

@Serializable
data class BonusProgressDto(
    val completed: Int = 0,
    val total: Int = 0,
    @SerialName("signal_lock") val signalLock: Int = 0,
    @SerialName("network_sync") val networkSync: Int = 0,
    @SerialName("full_uplink") val fullUplink: Int = 0,
    @SerialName("data_share_gb") val dataShareGb: Int = 0,
    @SerialName("data_share_soon") val dataShareSoon: Boolean = true,
)

@Serializable
data class BatteryOptimizationDto(
    @SerialName("should_show") val shouldShow: Boolean = false,
    @SerialName("declined_at") val declinedAt: String? = null,
    @SerialName("optimization_disabled") val optimizationDisabled: Boolean = false,
)

@Serializable
data class TapStartDto(
    @SerialName("network_type") val networkType: String,
    val vpn: Boolean = false,
    val emulator: Boolean = false,
    @SerialName("integrity_token") val integrityToken: String? = null,
)

@Serializable
data class TapFinishDto(
    val interrupted: Boolean = false,
    @SerialName("network_lost") val networkLost: Boolean = false,
    @SerialName("app_backgrounded") val appBackgrounded: Boolean = false,
)

@Serializable
data class TapStateDto(
    @SerialName("session_id") val sessionId: String? = null,
    val status: String,
    @SerialName("tap_count") val tapCount: Int = 0,
    @Serializable(DecimalAsLongSerializer::class)
    @SerialName("accrued_ion") val accruedIon: Long = 0,
    @Serializable(DecimalAsStringSerializer::class)
    @SerialName("spark_balance") val sparkBalance: String = "0",
    @SerialName("cooldown_available_at") val cooldownAvailableAt: String? = null,
    @SerialName("spark_window_rate") val sparkWindowRate: Int = 0,
)

@Serializable
data class IntegrityChallengeDto(
    @SerialName("request_hash") val requestHash: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class EarningStateUpdateDto(
    val vpn: Boolean? = null,
    val emulator: Boolean? = null,
)

@Serializable
data class EarningStateDto(
    val paused: Boolean = false,
    @SerialName("cooldown_available_at") val cooldownAvailableAt: String? = null,
    @Serializable(DecimalAsStringSerializer::class)
    @SerialName("spark_balance") val sparkBalance: String = "0",
)

@Serializable
data class HeartbeatDto(
    @SerialName("nodes_online") val nodesOnline: Int = 0,
    val stale: Boolean = false,
)

@Serializable
data class LocationUpdateDto(
    val vpn: Boolean = false,
)

@Serializable
data class CityDto(
    val city: String? = null,
)

@Serializable
data class MeshDto(
    @SerialName("glowing_cities") val glowingCities: List<String> = emptyList(),
    @SerialName("nodes_online") val nodesOnline: Int = 0,
    @SerialName("nodes_online_stale") val nodesOnlineStale: Boolean = false,
    @SerialName("nodes_online_updated_at") val nodesOnlineUpdatedAt: String? = null,
)

@Serializable
data class PushTokenRegisterDto(
    val token: String,
    val platform: String = "android",
)

@Serializable
data class PushTokenDeleteDto(
    val token: String,
)

@Serializable
data class PushTokenResponseDto(
    val registered: Boolean = false,
)

@Serializable
data class PushTokenDeleteResponseDto(
    val removed: Boolean = false,
)
