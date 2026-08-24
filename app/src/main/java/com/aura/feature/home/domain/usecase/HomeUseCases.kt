package com.aura.feature.home.domain.usecase

import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.domain.model.MeshState
import com.aura.feature.home.domain.repository.HomeRepository
import com.aura.feature.home.domain.repository.MeshRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveHomeStateUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<HomeState> = repository.observeHome()
}

class ObserveMeshStateUseCase @Inject constructor(
    private val repository: MeshRepository,
) {
    operator fun invoke(): Flow<MeshState> = repository.observeMesh()
}

class RefreshHomeUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
    private val meshRepository: MeshRepository,
) {
    suspend operator fun invoke(force: Boolean = false) {
        homeRepository.refresh()
        meshRepository.refresh(force)
    }
}

class SendHeartbeatUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke() = repository.sendHeartbeat()
}

class DeclineBatteryOptimizationUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke() = repository.declineBatteryOptimization()
}

class ConfirmBatteryOptimizationDisabledUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke() = repository.confirmBatteryOptimizationDisabled()
}

class RefreshBatteryOptimizationUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke() = repository.refreshBatteryOptimization()
}

class MarkBonusTeaserSeenUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke() = repository.markBonusTeaserSeen()
}
