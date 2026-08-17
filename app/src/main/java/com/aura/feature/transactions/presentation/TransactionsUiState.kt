package com.aura.feature.transactions.presentation

import androidx.compose.runtime.Immutable
import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.feature.transactions.domain.model.TransactionFilter

@Immutable
data class TransactionsUiState(
    val handle: String? = null,
    val hasUnreadNews: Boolean = false,
    val events: List<TransactionEvent> = emptyList(),
    val filter: TransactionFilter = TransactionFilter.ALL,
)

@Immutable
data class TransactionsActions(
    val onBackClick: () -> Unit = {},
    val onNewsClick: () -> Unit = {},
    val onFilterClick: (TransactionFilter) -> Unit = {},
)
