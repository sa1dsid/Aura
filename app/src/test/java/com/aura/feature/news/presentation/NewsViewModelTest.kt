package com.aura.feature.news.presentation

import com.aura.feature.news.FakeNewsRepository
import com.aura.feature.news.domain.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val FEED = listOf(
    NewsItem("1", "Data Share is coming", "body", 3L, read = false),
    NewsItem("2", "Second coupon window opens", "body", 2L, read = true),
    NewsItem("3", "Network update 1.2", "body", 1L, read = true),
)

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    private val repository = FakeNewsRepository(FEED)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening the drawer reports everything read but keeps the unread marks`() = runTest {
        val viewModel = collected(NewsViewModel(repository))

        viewModel.onDrawerOpened()

        assertEquals(1, repository.refreshCount)
        assertEquals(1, repository.markAllReadCount)
        assertFalse(repository.hasUnread.first())
        assertEquals(FEED, viewModel.uiState.value.items)
    }

    @Test
    fun `reopening the drawer drops the unread marks`() = runTest {
        val viewModel = collected(NewsViewModel(repository))
        viewModel.onDrawerOpened()

        viewModel.onDrawerDismissed()
        viewModel.onDrawerOpened()

        assertTrue(viewModel.uiState.value.items.all { it.read })
    }

    @Test
    fun `a card expands on tap and collapses on the second one`() = runTest {
        val viewModel = collected(NewsViewModel(repository))
        viewModel.onDrawerOpened()

        viewModel.onCardClick(FEED[0])
        assertEquals("1", viewModel.uiState.value.expandedId)

        viewModel.onCardClick(FEED[0])
        assertNull(viewModel.uiState.value.expandedId)
    }

    @Test
    fun `only one card stays open`() = runTest {
        val viewModel = collected(NewsViewModel(repository))
        viewModel.onDrawerOpened()

        viewModel.onCardClick(FEED[0])
        viewModel.onCardClick(FEED[2])

        assertEquals("3", viewModel.uiState.value.expandedId)
    }

    @Test
    fun `closing the drawer collapses the open card`() = runTest {
        val viewModel = collected(NewsViewModel(repository))
        viewModel.onDrawerOpened()
        viewModel.onCardClick(FEED[0])

        viewModel.onDrawerDismissed()

        assertNull(viewModel.uiState.value.expandedId)
    }

    private fun TestScope.collected(viewModel: NewsViewModel): NewsViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        return viewModel
    }
}
