package com.aura.feature.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aura.feature.network.domain.model.ConnectionDetails
import com.aura.feature.network.domain.model.NetworkMetrics
import com.aura.feature.network.domain.model.PingRecord
import com.aura.feature.network.domain.model.SpeedTestState
import com.aura.feature.network.presentation.NetworkUiState
import com.aura.feature.network.presentation.NetworkViewModel
import com.aura.feature.news.domain.model.NewsItem
import com.aura.testing.IntegrationTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
abstract class NetworkTestCase : IntegrationTestCase() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    internal fun network(
        news: List<NewsItem> = emptyList(),
        signedIn: Boolean = true,
        body: suspend CoroutineScope.(NetworkStack) -> Unit,
    ) = runBlocking {
        val stack = NetworkStack(context, news)
        stack.localStore.clearSession()
        if (signedIn) stack.signIn()
        try {
            body(stack)
        } finally {
            stack.close()
        }
    }

    internal fun screenOf(stack: NetworkStack): Pair<NetworkViewModel, List<NetworkUiState>> {
        val viewModel = stack.networkViewModel()
        val states = stack.contentOf(viewModel)
        viewModel.onScreenResumed()
        return viewModel to states
    }

    internal fun awaitContent(states: List<NetworkUiState>): NetworkUiState.Content {
        awaitUntil("the network screen to load") { states.any { it is NetworkUiState.Content } }
        return states.last { it is NetworkUiState.Content } as NetworkUiState.Content
    }

    internal fun awaitScreen(
        states: List<NetworkUiState>,
        what: String,
        condition: (NetworkUiState.Content) -> Boolean,
    ): NetworkUiState.Content {
        awaitUntil(what) {
            (states.lastOrNull { it is NetworkUiState.Content } as? NetworkUiState.Content)
                ?.let(condition) == true
        }
        return awaitContent(states)
    }

    internal fun awaitConnection(
        states: List<NetworkUiState>,
        what: String,
        condition: (ConnectionDetails) -> Boolean,
    ): ConnectionDetails = awaitScreen(states, what) { condition(it.connection) }.connection

    internal fun awaitMetrics(
        states: List<NetworkUiState>,
        what: String,
        condition: (NetworkMetrics) -> Boolean,
    ): NetworkMetrics = awaitScreen(states, what) { condition(it.metrics) }.metrics

    internal fun awaitHistory(
        states: List<NetworkUiState>,
        what: String,
        condition: (List<PingRecord>) -> Boolean,
    ): List<PingRecord> = awaitScreen(states, what) { condition(it.history) }.history

    internal fun awaitDiagnostics(
        states: List<SpeedTestState>,
        what: String,
        condition: (SpeedTestState) -> Boolean,
    ): SpeedTestState {
        awaitUntil(what) { states.lastOrNull()?.let(condition) == true }
        return states.last()
    }

    internal fun awaitJournal(
        stack: NetworkStack,
        what: String,
        condition: (List<PingRecord>) -> Boolean,
    ): List<PingRecord> {
        val records = stack.eventsOf(stack.pingHistory.observeHistory())
        awaitUntil(what) { records.lastOrNull()?.let(condition) == true }
        return records.last()
    }

    internal fun awaitRequest(
        stack: NetworkStack,
        path: String,
        method: String? = null,
        count: Int = 1,
    ) = awaitUntil("$count request(s) to ${method.orEmpty()} $path") {
        stack.server.hits(path, method) >= count
    }
}
