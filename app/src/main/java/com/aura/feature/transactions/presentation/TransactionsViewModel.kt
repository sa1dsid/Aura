package com.aura.feature.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.transactions.domain.model.TRANSACTIONS_LIMIT
import com.aura.feature.transactions.domain.model.TransactionFilter
import com.aura.feature.transactions.presentation.preview.TransactionsPreviewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    sessionStore: SessionStore,
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter.ALL)

    private val events = MutableStateFlow(TransactionsPreviewData.events.take(TRANSACTIONS_LIMIT))

    val uiState: StateFlow<TransactionsUiState> = combine(
        sessionStore.account,
        events,
        filter,
    ) { account, loaded, selected ->
        TransactionsUiState(
            handle = account?.handle,
            events = loaded,
            filter = selected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TransactionsUiState(),
    )

    fun onFilterClick(selected: TransactionFilter) {
        filter.value = selected
    }
}
