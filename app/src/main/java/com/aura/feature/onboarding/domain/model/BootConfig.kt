package com.aura.feature.onboarding.domain.model

data class BootConfig(
    val nodeCount: Int,
    val hotCities: List<String>,
) {
    companion object {
        const val DEFAULT_NODE_COUNT = 4_210
    }
}

enum class StartDestination { AUTH, INVITE, HOME }
