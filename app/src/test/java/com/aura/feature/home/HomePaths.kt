package com.aura.feature.home

internal object HomePaths {
    const val CONFIG = "/api/v1/home/config"
    const val DASHBOARD = "/api/v1/home/dashboard"
    const val MESH = "/api/v1/home/mesh"
    const val HEARTBEAT = "/api/v1/home/heartbeat"
    const val LOCATION = "/api/v1/home/location"
    const val EARNING_STATE = "/api/v1/home/earning-state"
    const val TAP_START = "/api/v1/home/tap/start"
    const val INTEGRITY = "/api/v1/home/tap/integrity-challenge"
    const val BATTERY = "/api/v1/home/battery-optimization"
    const val BATTERY_DECLINE = "/api/v1/home/battery-optimization/decline"
    const val BATTERY_DISABLED = "/api/v1/home/battery-optimization/confirmed-disabled"
    const val BONUS_TEASER_SEEN = "/api/v1/onboarding/bonus-teaser/seen"

    fun heartbeatOf(sessionId: String) = "/api/v1/home/tap/$sessionId/heartbeat"

    fun finishOf(sessionId: String) = "/api/v1/home/tap/$sessionId/finish"
}
