package com.aura.feature.ioni.presentation

import androidx.test.core.app.ApplicationProvider
import com.aura.core.api.AuraApi
import com.aura.core.api.dto.IoniConfigDto
import com.aura.core.api.dto.PublicConfigDto
import com.aura.core.config.AppConfigRepository
import com.aura.feature.ioni.data.IoniFaqRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class IoniViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var faqRepository: IoniFaqRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        faqRepository = IoniFaqRepository(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the ready questions arrive in the order the spec fixes`() = runTest(dispatcher) {
        val state = viewModel().uiState.first()

        assertEquals(15, state.questions.size)
        assertEquals("What is IO Aura?", state.questions.first())
        assertEquals("Still have a question?", state.questions.last())
    }

    @Test
    fun `a ready question thinks first and then types the answer out`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onQuestionClick(1)
        advanceTimeBy(100)

        assertEquals("Is IO Aura free?", viewModel.uiState.value.question)
        assertTrue(viewModel.uiState.value.isThinking)
        assertEquals("", viewModel.uiState.value.answer)

        advanceTimeBy(600)

        assertFalse(viewModel.uiState.value.isThinking)
        assertTrue(viewModel.uiState.value.answer.isNotEmpty())

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.answer.startsWith("Yes. IO Aura is completely free"))
    }

    @Test
    fun `a new question replaces the pair that is still typing`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onQuestionClick(0)
        advanceTimeBy(1_000)
        val firstAnswer = viewModel.uiState.value.answer
        assertTrue(firstAnswer.isNotEmpty())

        viewModel.onQuestionClick(2)
        advanceTimeBy(100)

        assertEquals("What is ION and why collect it?", viewModel.uiState.value.question)
        assertEquals("", viewModel.uiState.value.answer)
        assertTrue(viewModel.uiState.value.isThinking)
    }

    @Test
    fun `the free question field stops at two hundred characters`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onInputChange("x".repeat(250))
        advanceUntilIdle()

        assertEquals(IONI_INPUT_LIMIT, viewModel.uiState.value.input.length)
    }

    @Test
    fun `a free question is answered by the local stub and clears the field`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onInputChange("how does this work")
            viewModel.onSendClick()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("how does this work", state.question)
            assertEquals("", state.input)
            assertTrue(state.answer.startsWith("I’m still learning."))
        }

    @Test
    fun `the arrow is muffled while the planet spins`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onQuestionClick(0)
        advanceTimeBy(100)
        viewModel.onInputChange("second question")

        assertFalse(viewModel.uiState.value.isSendEnabled)

        viewModel.onSendClick()
        advanceTimeBy(100)

        assertEquals("What is IO Aura?", viewModel.uiState.value.question)
        assertEquals("second question", viewModel.uiState.value.input)
    }

    @Test
    fun `leaving the screen throws the pair away`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onQuestionClick(0)
        viewModel.onInputChange("draft")
        advanceTimeBy(1_000)

        viewModel.onScreenLeft()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.question)
        assertEquals("", state.answer)
        assertEquals("", state.input)
        assertFalse(state.isThinking)
    }

    @Test
    fun `the support address and the release flag come from the server config`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                config = IoniConfigDto(
                    supportEmail = "support@ioaura.app",
                    assistantEnabled = false,
                    aiReleased = true,
                ),
            )

            viewModel.onScreenResumed()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("support@ioaura.app", state.supportEmail)
            assertTrue(state.isAiReleased)
        }

    @Test
    fun `the typing speeds up as the answer grows`() {
        assertEquals(1, typingStep(0))
        assertEquals(1, typingStep(199))
        assertEquals(2, typingStep(200))
        assertEquals(4, typingStep(600))
        assertEquals(4, typingStep(5_000))
    }

    private fun TestScope.viewModel(config: IoniConfigDto = IoniConfigDto()): IoniViewModel {
        val api = mockk<AuraApi>()
        coEvery { api.publicConfig() } returns PublicConfigDto(ioni = config)

        val viewModel = IoniViewModel(
            faqRepository = faqRepository,
            appConfigRepository = AppConfigRepository(api = api, ioDispatcher = dispatcher),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        return viewModel
    }
}
