package com.aura.feature.promo.data.repository

import com.aura.core.api.dto.PromoDto
import com.aura.core.common.IoDispatcher
import com.aura.core.common.runCatchingCancellable
import com.aura.core.session.SessionCache
import com.aura.feature.promo.data.mapper.toDomain
import com.aura.feature.promo.data.remote.PromoCodesRemoteDataSource
import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.promo.domain.repository.PromoCodesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromoCodesRepositoryImpl @Inject constructor(
    private val remote: PromoCodesRemoteDataSource,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PromoCodesRepository, SessionCache {

    private val loaded = MutableStateFlow(emptyList<PromoCode>())

    override val promoCodes: Flow<List<PromoCode>> = loaded.asStateFlow()

    override suspend fun clearSession() {
        loaded.value = emptyList()
    }

    override suspend fun load(): Boolean = withContext(ioDispatcher) {
        val codes = runCatchingCancellable { remote.promoCodes() }.getOrNull()
            ?: return@withContext false

        loaded.value = codes.mapNotNull(PromoDto::toDomain)
        true
    }
}
