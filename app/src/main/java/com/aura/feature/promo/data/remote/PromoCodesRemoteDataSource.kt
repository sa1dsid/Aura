package com.aura.feature.promo.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.PromoDto
import javax.inject.Inject
import javax.inject.Singleton

interface PromoCodesRemoteDataSource {

    suspend fun promoCodes(): List<PromoDto>
}

@Singleton
class ApiPromoCodesRemoteDataSource @Inject constructor(
    private val api: AuraApi,
) : PromoCodesRemoteDataSource {

    override suspend fun promoCodes(): List<PromoDto> = api.promoCodes()
}
