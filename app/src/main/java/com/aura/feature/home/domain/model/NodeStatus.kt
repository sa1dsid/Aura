package com.aura.feature.home.domain.model

enum class NodeTier(val referralRate: Double) {
    IDLE_NODE(0.0),
    ACTIVE_SIGNAL(2.5),
    STABLE_LINK(2.5),
    CORE_NODE(5.0),
    IONIC_PRIME(10.0),
}

data class NodeStatus(
    val currentTier: NodeTier,
    val referralRate: Double,
    val progressToNext: Long,
    val progressTarget: Long?,
    val nextTier: NodeTier?,
)
