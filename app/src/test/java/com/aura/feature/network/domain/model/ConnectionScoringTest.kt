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

    @Test
    fun `rates a link that is bad on every count as poor`() {
        assertEquals(
            ConnectionGrade.POOR,
            ConnectionScoring.gradeOf(
                pingMs = 200,
                jitterMs = 500,
                packetLossPercent = 20.0,
                downloadMbps = 0.5,
            ),
        )
    }

    @Test
    fun `keeps a link with almost no bandwidth excellent when nothing else is wrong`() {
        assertEquals(
            ConnectionGrade.EXCELLENT,
            ConnectionScoring.gradeOf(
                pingMs = 19,
                jitterMs = 4,
                packetLossPercent = 0.0,
                downloadMbps = 0.5,
            ),
        )
    }

    @Test
    fun `keeps a link with wild jitter excellent when nothing else is wrong`() {
        assertEquals(
            ConnectionGrade.EXCELLENT,
            ConnectionScoring.gradeOf(
                pingMs = 19,
                jitterMs = 500,
                packetLossPercent = 0.0,
                downloadMbps = 90.0,
            ),
        )
    }

    @Test
    fun `lets the ping alone pull a perfect link down to good`() {
        assertEquals(
            ConnectionGrade.GOOD,
            ConnectionScoring.gradeOf(
                pingMs = 200,
                jitterMs = 4,
                packetLossPercent = 0.0,
                downloadMbps = 90.0,
            ),
        )
    }

    @Test
    fun `drops off excellent one notch below the line`() {
        assertEquals(
            ConnectionGrade.EXCELLENT,
            ConnectionScoring.gradeOf(19, 500, 0.0, 90.0),
        )
        assertEquals(
            ConnectionGrade.GOOD,
            ConnectionScoring.gradeOf(19, 500, 0.0, 30.0),
        )
    }

    @Test
    fun `counts a ping of twenty as already past ideal`() {
        assertEquals(ConnectionGrade.GOOD, ConnectionScoring.gradeOf(19, 44, 7.0, 5.0))
        assertEquals(ConnectionGrade.POOR, ConnectionScoring.gradeOf(20, 44, 7.0, 5.0))
    }

    @Test
    fun `counts a ping of thirty as already past good`() {
        assertEquals(ConnectionGrade.GOOD, ConnectionScoring.gradeOf(29, 44, 7.0, 30.0))
        assertEquals(ConnectionGrade.POOR, ConnectionScoring.gradeOf(30, 44, 7.0, 30.0))
    }

    @Test
    fun `counts a ping of forty five as already a warning`() {
        assertEquals(ConnectionGrade.GOOD, ConnectionScoring.gradeOf(44, 4, 7.0, 5.0))
        assertEquals(ConnectionGrade.POOR, ConnectionScoring.gradeOf(45, 4, 7.0, 5.0))
    }

    @Test
    fun `steps the jitter at five milliseconds`() {
        assertEquals(ConnectionGrade.GOOD, ConnectionScoring.gradeOf(92, 4, 7.0, 90.0))
        assertEquals(ConnectionGrade.POOR, ConnectionScoring.gradeOf(92, 5, 7.0, 90.0))
    }

    @Test
    fun `steps the packet loss at a tenth of a percent`() {
        assertEquals(ConnectionGrade.GOOD, ConnectionScoring.gradeOf(92, 44, 0.09, 30.0))
        assertEquals(ConnectionGrade.POOR, ConnectionScoring.gradeOf(92, 44, 0.1, 30.0))
    }

    @Test
    fun `steps the download at twenty five megabits`() {
        assertEquals(ConnectionGrade.GOOD, ConnectionScoring.gradeOf(92, 44, 0.0, 25.0))
        assertEquals(ConnectionGrade.POOR, ConnectionScoring.gradeOf(92, 44, 0.0, 24.9))
    }

    @Test
    fun `carries the score and the dots the card shows`() {
        assertEquals(5.0, ConnectionGrade.EXCELLENT.score, 0.0001)
        assertEquals(3.0, ConnectionGrade.GOOD.score, 0.0001)
        assertEquals(1.0, ConnectionGrade.POOR.score, 0.0001)
    }
}
