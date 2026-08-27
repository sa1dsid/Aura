package com.aura.feature.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.aura.core.api.RoutingApiServer
import com.aura.feature.news.FakeNewsRepository
import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.promo.data.remote.ApiPromoCodesRemoteDataSource
import com.aura.feature.promo.data.repository.PromoCodesRepositoryImpl
import com.aura.feature.promo.presentation.PromoCodesUiState
import com.aura.feature.promo.presentation.PromoCodesViewModel
import com.aura.feature.terminal.data.remote.ApiTerminalRemoteDataSource
import com.aura.feature.terminal.data.repository.TerminalRepositoryImpl
import com.aura.feature.terminal.presentation.TerminalUiState
import com.aura.feature.terminal.presentation.TerminalViewModel
import com.aura.feature.transactions.data.remote.ApiTransactionsRemoteDataSource
import com.aura.feature.transactions.data.repository.TransactionsRepositoryImpl
import com.aura.feature.transactions.presentation.TransactionsUiState
import com.aura.feature.transactions.presentation.TransactionsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections

internal fun terminalAccount(handle: String = "syrex") = Account(
    id = "39",
    email = "smoke@auratest.dev",
    handle = handle,
    inviteLink = "https://io-aura.example/i/syrex",
    authProvider = AuthProvider.EMAIL,
)

internal fun unreadNews(): List<NewsItem> = listOf(
    NewsItem(
        id = "1",
        title = "Mesh update",
        body = "The mesh grew by a thousand nodes.",
        publishedAt = 1_787_702_400_000L,
        read = false,
    )
)

internal class TerminalStack(news: List<NewsItem> = emptyList()) {

    val server = RoutingApiServer()

    private val ioDispatcher = Dispatchers.Unconfined

    private val stackScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val viewModelStore = ViewModelStore()

    private val tracked = mutableListOf<ViewModel>()

    val sessionStore = SessionStore()

    val newsRepository = FakeNewsRepository(news)

    init {
        server.always(TerminalPaths.TERMINAL, body = Terminal.counters())
        server.always(TerminalPaths.TRANSACTIONS, body = "[]")
        server.always(TerminalPaths.PROMO_CODES, body = "[]")
    }

    val repository = TerminalRepositoryImpl(
        remote = ApiTerminalRemoteDataSource(server.api),
        ioDispatcher = ioDispatcher,
    )

    val transactionsRepository = TransactionsRepositoryImpl(
        remote = ApiTransactionsRemoteDataSource(server.api),
        ioDispatcher = ioDispatcher,
    )

    val promoCodesRepository = PromoCodesRepositoryImpl(
        remote = ApiPromoCodesRemoteDataSource(server.api),
        ioDispatcher = ioDispatcher,
    )

    fun signIn(handle: String = "syrex") = sessionStore.open(terminalAccount(handle))

    fun <T> eventsOf(flow: Flow<T>): List<T> {
        val collected = Collections.synchronizedList(mutableListOf<T>())
        stackScope.launch { flow.collect { collected += it } }
        return collected
    }

    fun terminalViewModel(): TerminalViewModel = track(
        "terminal",
        TerminalViewModel(terminalRepository = repository, newsRepository = newsRepository),
    )

    fun transactionsViewModel(): TransactionsViewModel = track(
        "transactions",
        TransactionsViewModel(
            transactionsRepository = transactionsRepository,
            terminalRepository = repository,
            newsRepository = newsRepository,
            sessionStore = sessionStore,
        ),
    )

    fun promoCodesViewModel(): PromoCodesViewModel = track(
        "promo",
        PromoCodesViewModel(
            promoCodesRepository = promoCodesRepository,
            terminalRepository = repository,
            newsRepository = newsRepository,
            sessionStore = sessionStore,
        ),
    )

    fun contentOf(viewModel: TerminalViewModel): List<TerminalUiState> = eventsOf(viewModel.uiState)

    fun contentOf(viewModel: TransactionsViewModel): List<TransactionsUiState> =
        eventsOf(viewModel.uiState)

    fun contentOf(viewModel: PromoCodesViewModel): List<PromoCodesUiState> =
        eventsOf(viewModel.uiState)

    suspend fun close() {
        withTimeoutOrNull(SHUTDOWN_MILLIS) {
            tracked.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
            stackScope.coroutineContext.job.cancelAndJoin()
        }
        viewModelStore.clear()
        server.shutdown()
    }

    private fun <T : ViewModel> track(key: String, viewModel: T): T {
        viewModelStore.put(key, viewModel)
        tracked += viewModel
        return viewModel
    }

    private companion object {
        const val SHUTDOWN_MILLIS = 5_000L
    }
}
