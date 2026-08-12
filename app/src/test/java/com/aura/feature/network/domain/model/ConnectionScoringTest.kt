package com.aura.feature.network.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionScoringTest {

    @Test
    fun `rates a clean fast link as excellent`() {
        assertEquals(
            ConnectionGrade.EXCELLENT,
            ConnectionScoring.gradeOf(
                pingMs = 14,
                jitterMs = 3,
                packetLossPercent = 0.0,
                downloadMbps = 96.0,
            ),
        )
    }

    @Test
    fun `rates a middling link as good`() {
        assertEquals(
            ConnectionGrade.GOOD,
            ConnectionScoring.gradeOf(
                pingMs = 28,
                jitterMs = 9,
                packetLossPercent = 0.4,
                downloadMbps = 32.0,
            ),
        )
    }

    @Test
    fun `rates a slow lossy link as poor`() {
        assertEquals(
            ConnectionGrade.POOR,
            ConnectionScoring.gradeOf(
                pingMs = 92,
                jitterMs = 44,
                packetLossPercent = 6.0,
                downloadMbps = 2.0,
            ),
        )
    }

    @Test
    fun `lets packet loss alone pull a fast link down`() {
        assertEquals(
            ConnectionGrade.GOOD,
            ConnectionScoring.gradeOf(
                pingMs = 15,
                jitterMs = 4,
                packetLossPercent = 7.0,
                downloadMbps = 90.0,
            ),
        )
    }
}
