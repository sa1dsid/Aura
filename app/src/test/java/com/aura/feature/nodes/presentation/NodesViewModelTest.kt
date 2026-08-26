package com.aura.feature.nodes.presentation

import com.aura.feature.news.FakeNewsRepository
import com.aura.feature.news.domain.model.NewsItem
import com.aura.feature.nodes.RecordingNodesRepository
import com.aura.feature.nodes.domain.usecase.ObserveNodesStateUseCase
import com.aura.feature.nodes.domain.usecase.RefreshNodesUseCase
import com.aura.feature.nodes.nodesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val UNREAD = listOf(NewsItem("1", "Mesh update", "body", 3L, read = false))

@OptIn(ExperimentalCoroutinesApi::class)
class NodesViewModelTest {

    private val repository = RecordingNodesRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the screen is loading until the first snapshot arrives`() = runTest {
        val viewModel = collected(viewModelOf())

        assertEquals(NodesUiState.Loading, viewModel.uiState.value)

        repository.emit(nodesState())

        assertTrue(viewModel.uiState.value is NodesUiState.Content)
    }

    @Test
    fun `the view model refreshes as soon as it is created`() = runTest {
        collected(viewModelOf())

        assertEquals(1, repository.refreshes)
    }

    @Test
    fun `coming back to the screen refreshes again`() = runTest {
        val viewModel = collected(viewModelOf())

        viewModel.onScreenResumed()
        viewModel.onScreenResumed()

        assertEquals(3, repository.refreshes)
    }

    @Test
    fun `the snapshot and the news badge arrive together`() = runTest {
        val viewModel = collected(viewModelOf(FakeNewsRepository(UNREAD)))

        repository.emit(nodesState(friendsJoined = 6))

        val content = viewModel.uiState.value as NodesUiState.Content
        assertEquals(6, content.nodes.friendsJoined)
        assertTrue(content.hasUnreadNews)
    }

    @Test
    fun `reading the news puts the badge out`() = runTest {
        val news = FakeNewsRepository(UNREAD)
        val viewModel = collected(viewModelOf(news))
        repository.emit(nodesState())

        news.markAllRead()

        assertFalse((viewModel.uiState.value as NodesUiState.Content).hasUnreadNews)
    }

    @Test
    fun `a new snapshot replaces the one on the screen`() = runTest {
        val viewModel = collected(viewModelOf())
        repository.emit(nodesState(friendsJoined = 1))

        repository.emit(nodesState(friendsJoined = 7, activeFriends = 3))

        val content = viewModel.uiState.value as NodesUiState.Content
        assertEquals(7, content.nodes.friendsJoined)
        assertEquals(3, content.nodes.activeFriends)
    }

    private fun viewModelOf(
        news: FakeNewsRepository = FakeNewsRepository(),
    ) = NodesViewModel(
        observeNodes = ObserveNodesStateUseCase(repository),
        newsRepository = news,
        refreshNodes = RefreshNodesUseCase(repository),
    )

    private fun TestScope.collected(viewModel: NodesViewModel): NodesViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        return viewModel
    }
}
