package com.aura.feature.home.domain.model

enum class NodeTier {
    IDLE_NODE,
    ACTIVE_SIGNAL,
    STABLE_LINK,
    CORE_NODE,
    IONIC_PRIME,
}

data class NodeStatus(
    val currentTier: NodeTier,
    val referralRate: Double,
    val progressToNext: Long,
    val progressTarget: Long?,
    val nextTier: NodeTier?,
    val rateMultiplierPercent: Int = 100,
    val sparkReferralPercent: Int = 0,
)
