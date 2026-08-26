package com.aura.feature.nodes

internal object Nodes {

    const val TIER_IDLE = "idle"
    const val TIER_ACTIVE_SIGNAL = "active_signal"
    const val TIER_STABLE_LINK = "stable_link"
    const val TIER_CORE_NODE = "core_node"
    const val TIER_IONIC_PRIME = "ionic_prime"

    const val STATUS_EARNING = "EARNING"
    const val STATUS_SPARK_ONLY = "SPARK_ONLY"
    const val STATUS_INACTIVE = "INACTIVE"

    const val PERSONAL_CODE = "K69VL9R7"
    const val PERSONAL_URL = "https://io-aura.example/invite?code=K69VL9R7"
    const val SHARE_TEXT = "Join my node on IO Aura: https://io-aura.example/invite?code=K69VL9R7"
    const val ACCOUNT_LINK = "https://io-aura.example/i/syrex"

    fun snapshot(
        friendsJoined: Int = 0,
        activeFriends: Int = 0,
        tier: String = TIER_IDLE,
        nextThreshold: Int? = 6,
        moreForNextTier: Int? = null,
        nodeStatus: String = nodeStatus(),
        sparkReferralPercent: Int = 0,
        ionReferralPercentStage2: Int = 0,
        earnedSpark: String = "0.000000",
        earnedIon: Long = 0,
        friends: List<String> = emptyList(),
    ): String = """
        {"friends_joined":$friendsJoined,"active_friends":$activeFriends,"tier":"$tier",
        "next_threshold":${nextThreshold ?: "null"},
        "more_for_next_tier":${moreForNextTier ?: "null"},
        "node_status":$nodeStatus,
        "spark_referral_percent":$sparkReferralPercent,
        "ion_referral_percent_stage_2":$ionReferralPercentStage2,
        "earned_from_referrals":{"spark":"$earnedSpark","ion":$earnedIon},
        "friends":${friends.joinToString(prefix = "[", postfix = "]")}}
    """.trimIndent()

    fun nodeStatus(
        tier: String = TIER_IDLE,
        progressMetric: String = "own_taps",
        progressCurrent: Int = 0,
        progressTarget: Int? = null,
        rateMultiplierPercent: Int = 100,
        sparkReferralPercent: Int = 0,
        ionReferralPercentStage2: Int = 0,
        activeFriends: Int? = null,
    ): String = """
        {"tier":"$tier","progress_metric":"$progressMetric",
        "progress_current":$progressCurrent,
        "progress_target":${progressTarget ?: "null"},
        "rate_multiplier_percent":$rateMultiplierPercent,
        "spark_referral_percent":$sparkReferralPercent,
        "ion_referral_percent_stage_2":$ionReferralPercentStage2,
        "active_friends":${activeFriends ?: "null"}}
    """.trimIndent()

    fun friend(
        id: Int = 1,
        displayName: String = "Alex K.",
        status: String = STATUS_EARNING,
        ownSpark: String = "0.000000",
        ownIon: Long = 0,
        tapCount: Int = 0,
    ): String = """
        {"id":$id,"display_name":"$displayName","status":"$status",
        "own_spark":"$ownSpark","own_ion":$ownIon,"tap_count":$tapCount}
    """.trimIndent()

    fun invite(
        decision: String = "applied",
        appliedCode: String? = "IDF46VS0",
        source: String? = "manual",
        personalCode: String = PERSONAL_CODE,
        personalUrl: String = PERSONAL_URL,
        shareText: String = SHARE_TEXT,
    ): String = """
        {"decision":"$decision",
        "applied_code":${appliedCode?.let { "\"$it\"" } ?: "null"},
        "source":${source?.let { "\"$it\"" } ?: "null"},
        "personal_code":"$personalCode","personal_url":"$personalUrl",
        "share_text":"$shareText"}
    """.trimIndent()

    fun config(links: List<Pair<String, String>> = emptyList()): String {
        val social = links.joinToString(prefix = "[", postfix = "]") { (kind, url) ->
            "{\"kind\":\"$kind\",\"url\":\"$url\"}"
        }
        return """
            {"terms_url":"https://io-aura.example/terms",
            "privacy_url":"https://io-aura.example/privacy",
            "feature_flags":{"data_share":false,"traffic_withdrawals":false,"vpn_code":false},
            "social_links":$social,
            "ioni":{"support_email":"","assistant_enabled":false,"ai_released":false}}
        """.trimIndent()
    }

    fun allSocials(): List<Pair<String, String>> = listOf(
        "discord" to "https://discord.gg/ioaura",
        "telegram" to "https://t.me/ioaura",
        "x" to "https://x.com/ioaura",
        "reddit" to "https://reddit.com/r/ioaura",
        "instagram" to "https://instagram.com/ioaura",
        "snapchat" to "https://snapchat.com/add/ioaura",
    )
}
