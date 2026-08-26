package com.aura.feature.promo

import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.promo.domain.repository.PromoCodesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakePromoCodesRepository(
    private val stored: List<PromoCode> = emptyList(),
) : PromoCodesRepository {

    private val loaded = MutableStateFlow(emptyList<PromoCode>())

    var failNextLoad = false

    var gate: CompletableDeferred<Unit>? = null

    var loadCount = 0
        private set

    override val promoCodes: Flow<List<PromoCode>> = loaded

    override suspend fun load(): Boolean {
        loadCount++
        gate?.await()
        if (failNextLoad) {
            failNextLoad = false
            return false
        }

        loaded.value = stored
        return true
    }
}
