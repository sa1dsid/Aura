package com.aura.feature.promo.domain.repository

import com.aura.feature.promo.domain.model.PromoCode
import kotlinx.coroutines.flow.Flow

interface PromoCodesRepository {

    val promoCodes: Flow<List<PromoCode>>

    suspend fun load(): Boolean
}
