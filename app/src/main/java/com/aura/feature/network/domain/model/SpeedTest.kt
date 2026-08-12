package com.aura.feature.network.domain.model

enum class ConnectionGrade(val score: Double, val filledDots: Int) {
    EXCELLENT(5.0, 5),
    GOOD(3.0, 3),
    POOR(1.0, 1),
}

data class SpeedTestResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Int,
    val jitterMs: Int,
    val packetLossPercent: Double,
    val grade: ConnectionGrade,
)

sealed interface SpeedTestState {

    data object Idle : SpeedTestState

    data class Running(val progress: Float) : SpeedTestState

    data class Done(val result: SpeedTestResult) : SpeedTestState
}

enum class SpeedTestFailure { NO_CONNECTION, INTERRUPTED }
