package com.aura.feature.network.presentation.components

import com.aura.feature.network.domain.model.ConnectionGrade
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.domain.model.SpeedTestState
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticsGaugeTest {

    @Test
    fun `a waiting gauge is dark`() {
        assertEquals(0f, SpeedTestState.Idle.litFraction(), 0.0001f)
    }

    @Test
    fun `a running gauge is lit as far as the test has come`() {
        assertEquals(0.42f, SpeedTestState.Running(0.42f).litFraction(), 0.0001f)
    }

    @Test
    fun `a gauge at the start of a test is dark`() {
        assertEquals(0f, SpeedTestState.Running(0f).litFraction(), 0.0001f)
    }

    @Test
    fun `a finished gauge is lit all the way`() {
        assertEquals(1f, SpeedTestState.Done(result()).litFraction(), 0.0001f)
    }

    private fun result() = SpeedTestResult(
        downloadMbps = 48.6,
        uploadMbps = 12.4,
        pingMs = 24,
        jitterMs = 6,
        packetLossPercent = 0.2,
        grade = ConnectionGrade.EXCELLENT,
    )
}
