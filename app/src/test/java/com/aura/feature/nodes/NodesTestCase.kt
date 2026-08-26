package com.aura.feature.nodes

import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.presentation.NodesUiState
import com.aura.feature.nodes.presentation.NodesViewModel
import com.aura.testing.IntegrationTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

abstract class NodesTestCase : IntegrationTestCase() {

    internal fun nodes(
        news: List<NewsItem> = emptyList(),
        signedIn: Boolean = true,
        body: suspend CoroutineScope.(NodesStack) -> Unit,
    ) = runBlocking {
        val stack = NodesStack(news)
        if (signedIn) stack.signIn()
        try {
            body(stack)
        } finally {
            stack.close()
        }
    }

    internal fun awaitContent(states: List<NodesUiState>): NodesUiState.Content {
        awaitUntil("the nodes screen to load") { states.any { it is NodesUiState.Content } }
        return states.last { it is NodesUiState.Content } as NodesUiState.Content
    }

    internal fun awaitNodes(
        states: List<NodesUiState>,
        what: String,
        condition: (NodesState) -> Boolean,
    ): NodesState {
        awaitUntil(what) {
            states.lastOrNull { it is NodesUiState.Content }
                ?.let { condition((it as NodesUiState.Content).nodes) } == true
        }
        return awaitContent(states).nodes
    }

    internal fun awaitRequest(stack: NodesStack, path: String, count: Int = 1) =
        awaitUntil("$count request(s) to $path") { stack.server.hits(path) >= count }

    internal fun screenOf(stack: NodesStack): Pair<NodesViewModel, List<NodesUiState>> {
        val viewModel = stack.nodesViewModel()
        return viewModel to stack.contentOf(viewModel)
    }
}
