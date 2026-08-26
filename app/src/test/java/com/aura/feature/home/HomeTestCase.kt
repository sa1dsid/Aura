package com.aura.feature.home

import com.aura.feature.home.domain.model.HomeState
import com.aura.feature.home.presentation.HomeUiState
import com.aura.feature.home.presentation.HomeViewModel
import com.aura.testing.IntegrationTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

abstract class HomeTestCase : IntegrationTestCase() {

    internal fun home(
        isEmulator: Boolean = false,
        body: suspend CoroutineScope.(HomeStack) -> Unit,
    ) = runBlocking {
        val stack = HomeStack(isEmulator)
        try {
            body(stack)
        } finally {
            stack.close()
        }
    }

    internal fun awaitContent(states: List<HomeUiState>): HomeUiState.Content {
        awaitUntil("the home screen to load") { states.any { it is HomeUiState.Content } }
        return states.last { it is HomeUiState.Content } as HomeUiState.Content
    }

    internal fun awaitHome(
        states: List<HomeUiState>,
        what: String,
        condition: (HomeState) -> Boolean,
    ): HomeState {
        awaitUntil(what) {
            states.lastOrNull { it is HomeUiState.Content }
                ?.let { condition((it as HomeUiState.Content).home) } == true
        }
        return awaitContent(states).home
    }

    internal fun awaitRequest(stack: HomeStack, path: String, count: Int = 1) =
        awaitUntil("$count request(s) to $path") { stack.server.hits(path) >= count }

    internal fun screenOf(stack: HomeStack): Pair<HomeViewModel, List<HomeUiState>> {
        val viewModel = stack.homeViewModel()
        return viewModel to stack.contentOf(viewModel)
    }
}
