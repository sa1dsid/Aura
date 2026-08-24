package com.aura.feature.home.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.BatteryOptimizationDto
import com.aura.core.api.dto.DashboardDto
import com.aura.core.api.dto.EarningStateDto
import com.aura.core.api.dto.EarningStateUpdateDto
import com.aura.core.api.dto.HeartbeatDto
import com.aura.core.api.dto.LocationUpdateDto
import com.aura.core.api.dto.MeshDto
import com.aura.core.api.dto.TapFinishDto
import com.aura.core.api.dto.TapStartDto
import com.aura.core.api.dto.TapStateDto
import javax.inject.Inject
import javax.inject.Singleton

interface HomeRemoteDataSource {

    suspend fun dashboard(): DashboardDto

    suspend fun startTap(networkType: String, vpn: Boolean, emulator: Boolean): TapStateDto

    suspend fun finishTap(
        sessionId: String,
        interrupted: Boolean,
        networkLost: Boolean,
        appBackgrounded: Boolean,
    ): TapStateDto

    suspend fun updateEarningState(vpn: Boolean?, emulator: Boolean?): EarningStateDto

    suspend fun batteryOptimization(): BatteryOptimizationDto

    suspend fun declineBatteryOptimization(): BatteryOptimizationDto

    suspend fun confirmBatteryOptimizationDisabled(): BatteryOptimizationDto

    suspend fun heartbeat(): HeartbeatDto

    suspend fun updateLocation(vpn: Boolean): String?

    suspend fun mesh(): MeshDto
}

@Singleton
class ApiHomeRemoteDataSource @Inject constructor(
    private val api: AuraApi,
) : HomeRemoteDataSource {

    override suspend fun dashboard(): DashboardDto = api.dashboard()

    override suspend fun startTap(
        networkType: String,
        vpn: Boolean,
        emulator: Boolean,
    ): TapStateDto {
        val integrityToken = runCatching { api.issueIntegrityChallenge().requestHash }.getOrNull()

        return api.startTap(
            TapStartDto(
                networkType = networkType,
                vpn = vpn,
                emulator = emulator,
                integrityToken = integrityToken,
            )
        )
    }

    override suspend fun finishTap(
        sessionId: String,
        interrupted: Boolean,
        networkLost: Boolean,
        appBackgrounded: Boolean,
    ): TapStateDto = api.finishTap(
        sessionId = sessionId,
        request = TapFinishDto(
            interrupted = interrupted,
            networkLost = networkLost,
            appBackgrounded = appBackgrounded,
        ),
    )

    override suspend fun updateEarningState(vpn: Boolean?, emulator: Boolean?): EarningStateDto =
        api.updateEarningState(EarningStateUpdateDto(vpn = vpn, emulator = emulator))

    override suspend fun batteryOptimization(): BatteryOptimizationDto = api.batteryOptimization()

    override suspend fun declineBatteryOptimization(): BatteryOptimizationDto =
        api.declineBatteryOptimization()

    override suspend fun confirmBatteryOptimizationDisabled(): BatteryOptimizationDto =
        api.confirmBatteryOptimizationDisabled()

    override suspend fun heartbeat(): HeartbeatDto = api.heartbeat()

    override suspend fun updateLocation(vpn: Boolean): String? =
        api.updateLocation(LocationUpdateDto(vpn = vpn)).city

    override suspend fun mesh(): MeshDto = api.mesh()
}
