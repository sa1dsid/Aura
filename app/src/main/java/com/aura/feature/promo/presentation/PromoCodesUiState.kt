package com.aura.feature.promo.presentation

import androidx.compose.runtime.Immutable
import com.aura.feature.promo.domain.model.PromoCode

@Immutable
data class PromoCodesUiState(
    val handle: String? = null,
    val hasUnreadNews: Boolean = false,
    val codes: List<PromoCode> = emptyList(),
)

@Immutable
data class PromoCodesActions(
    val onBackClick: () -> Unit = {},
    val onNewsClick: () -> Unit = {},
    val onCodeClick: (PromoCode) -> Unit = {},
)
