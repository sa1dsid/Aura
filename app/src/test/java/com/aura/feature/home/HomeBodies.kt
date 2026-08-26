package com.aura.feature.home

internal const val CLOCK_START = 1_787_702_400_000L

internal object Home {

    const val TIER_IDLE = "idle"

    fun dashboard(
        accruedIon: Long = 20,
        availableToWithdrawIon: Long = 0,
        reservedBonusIon: Long = 3_000,
        sparkBalance: String = "0.000000",
        sparkIssued: Int = 0,
        sparkLimit: Int = 4,
        sparkCompleted: Boolean = false,
        tapCount: Int = 1,
        cooldownAvailableAt: String? = null,
        node: String = node(),
        bonus: String = bonus(),
        bonusTeaserShouldBlink: Boolean = false,
        unreadNews: Int = 0,
        battery: String = battery(),
    ): String = """
        {"accrued_ion":$accruedIon,
        "available_to_withdraw_ion":$availableToWithdrawIon,
        "reserved_bonus_ion":$reservedBonusIon,
        "spark_balance":"$sparkBalance",
        "spark_coupon_campaign":{"issued":$sparkIssued,"limit":$sparkLimit,
        "completed":$sparkCompleted},
        "tap_count":$tapCount,
        "cooldown_available_at":${cooldownAvailableAt?.let { "\"$it\"" } ?: "null"},
        "node":$node,"bonus":$bonus,
        "gift_popup_should_show":false,
        "bonus_teaser_should_blink":$bonusTeaserShouldBlink,
        "unread_news":$unreadNews,
        "battery_optimization":$battery,
        "ioni_state":"coming","ioni_last_completed_tap":null}
    """.trimIndent()

    fun node(
        tier: String = TIER_IDLE,
        progressMetric: String = "own_taps",
        progressCurrent: Int = 1,
        progressTarget: Int? = 6,
        rateMultiplierPercent: Int = 100,
        sparkReferralPercent: Int = 0,
        activeFriends: Int? = 0,
    ): String = """
        {"tier":"$tier","progress_metric":"$progressMetric",
        "progress_current":$progressCurrent,
        "progress_target":${progressTarget ?: "null"},
        "rate_multiplier_percent":$rateMultiplierPercent,
        "spark_referral_percent":$sparkReferralPercent,
        "ion_referral_percent_stage_2":0,
        "active_friends":${activeFriends ?: "null"}}
    """.trimIndent()

    fun bonus(
        completed: Int = 0,
        total: Int = 4,
        signalLock: Int = 1,
        networkSync: Int = 0,
        fullUplink: Int = 1,
        dataShareGb: Int = 0,
        dataShareSoon: Boolean = true,
    ): String = """
        {"completed":$completed,"total":$total,"signal_lock":$signalLock,
        "network_sync":$networkSync,"full_uplink":$fullUplink,
        "data_share_gb":$dataShareGb,"data_share_soon":$dataShareSoon}
    """.trimIndent()

    fun battery(
        shouldShow: Boolean = false,
        declinedAt: String? = null,
        optimizationDisabled: Boolean = false,
    ): String = """
        {"should_show":$shouldShow,
        "declined_at":${declinedAt?.let { "\"$it\"" } ?: "null"},
        "optimization_disabled":$optimizationDisabled}
    """.trimIndent()

    fun mesh(
        nodesOnline: Int = 2,
        cities: List<String> = listOf("Tallinn"),
        stale: Boolean = false,
    ): String = """
        {"glowing_cities":${cities.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
        "nodes_online":$nodesOnline,"nodes_online_stale":$stale,
        "nodes_online_updated_at":"2026-08-24T16:42:08.738817Z"}
    """.trimIndent()

    fun tap(
        sessionId: String? = "session-1",
        status: String = "running",
        tapCount: Int = 0,
        accruedIon: Long = 0,
        sparkBalance: String = "0.000000",
        cooldownAvailableAt: String? = null,
        sparkWindowRate: Int = 0,
    ): String = """
        {"session_id":${sessionId?.let { "\"$it\"" } ?: "null"},"status":"$status",
        "tap_count":$tapCount,"accrued_ion":$accruedIon,
        "spark_balance":"$sparkBalance",
        "cooldown_available_at":${cooldownAvailableAt?.let { "\"$it\"" } ?: "null"},
        "spark_window_rate":$sparkWindowRate}
    """.trimIndent()

    fun earningState(paused: Boolean = false, cooldownAvailableAt: String? = null): String = """
        {"paused":$paused,
        "cooldown_available_at":${cooldownAvailableAt?.let { "\"$it\"" } ?: "null"},
        "spark_balance":"0.000000"}
    """.trimIndent()

    fun integrity(requestHash: String = "integrity-hash"): String =
        """{"request_hash":"$requestHash","expires_at":"2026-08-26T00:05:00Z"}"""

    fun heartbeat(): String = """{"ok":true}"""

    fun city(name: String = "Tallinn"): String = """{"city":"$name"}"""

    fun detail(text: String): String = """{"detail":"$text"}"""
}
