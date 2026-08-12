package com.aura.feature.network.domain.model

private const val PING_WEIGHT = 0.35

private const val JITTER_WEIGHT = 0.20

private const val PACKET_LOSS_WEIGHT = 0.25

private const val DOWNLOAD_WEIGHT = 0.20

private const val EXCELLENT_THRESHOLD = 4.2

private const val GOOD_THRESHOLD = 2.6

private val PING_STEPS_MS = listOf(PING_IDEAL_MS, PING_GOOD_MS, PING_WARNING_MS, 80)

private val JITTER_STEPS_MS = listOf(5, 10, 20, 40)

private val PACKET_LOSS_STEPS_PERCENT = listOf(0.1, 0.5, 1.5, 5.0)

private val DOWNLOAD_STEPS_MBPS = listOf(50.0, 25.0, 10.0, 3.0)

object ConnectionScoring {

    fun gradeOf(
        pingMs: Int,
        jitterMs: Int,
        packetLossPercent: Double,
        downloadMbps: Double,
    ): ConnectionGrade {
        val score = PING_WEIGHT * lowerIsBetter(pingMs.toDouble(), PING_STEPS_MS.map(Int::toDouble)) +
            JITTER_WEIGHT * lowerIsBetter(jitterMs.toDouble(), JITTER_STEPS_MS.map(Int::toDouble)) +
            PACKET_LOSS_WEIGHT * lowerIsBetter(packetLossPercent, PACKET_LOSS_STEPS_PERCENT) +
            DOWNLOAD_WEIGHT * higherIsBetter(downloadMbps, DOWNLOAD_STEPS_MBPS)

        return when {
            score >= EXCELLENT_THRESHOLD -> ConnectionGrade.EXCELLENT
            score >= GOOD_THRESHOLD -> ConnectionGrade.GOOD
            else -> ConnectionGrade.POOR
        }
    }

    private fun lowerIsBetter(value: Double, steps: List<Double>): Double =
        (steps.size + 1 - steps.count { value >= it }).toDouble()

    private fun higherIsBetter(value: Double, steps: List<Double>): Double =
        (steps.size + 1 - steps.count { value < it }).toDouble()
}
