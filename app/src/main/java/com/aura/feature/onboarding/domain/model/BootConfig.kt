package com.aura.feature.onboarding.domain.model

data class BootConfig(val nodeCount: Int) {
    companion object {
        const val DEFAULT_NODE_COUNT = 4_210

        val FALLBACK = BootConfig(DEFAULT_NODE_COUNT)
    }
}

enum class StartDestination { AUTH, INVITE, HOME }
