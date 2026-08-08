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
