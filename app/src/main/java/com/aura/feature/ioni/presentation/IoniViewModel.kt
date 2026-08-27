package com.aura.feature.ioni.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.config.AppConfigRepository
import com.aura.feature.ioni.data.IoniFaqRepository
import com.aura.feature.ioni.domain.model.FaqEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val THINKING_MILLIS = 500L

private const val TYPING_TICK_MILLIS = 25L

private const val TYPING_RAMP_CHARS = 200

private const val TYPING_MAX_STEP = 4

@HiltViewModel
class IoniViewModel @Inject constructor(
    private val faqRepository: IoniFaqRepository,
    private val appConfigRepository: AppConfigRepository,
) : ViewModel() {

    private val entries: List<FaqEntry> = faqRepository.entries()

    private val local = MutableStateFlow(IoniUiState(questions = entries.map(FaqEntry::question)))

    private var answerJob: Job? = null

    val uiState: StateFlow<IoniUiState> =
        combine(local, appConfigRepository.config) { state, config ->
            state.copy(
                supportEmail = config.ioni.supportEmail,
                isAiReleased = config.ioni.aiReleased,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = local.value,
        )

    fun onScreenResumed() {
        viewModelScope.launch { appConfigRepository.refresh() }
    }

    fun onScreenLeft() {
        answerJob?.cancel()
        answerJob = null
        local.value = IoniUiState(questions = entries.map(FaqEntry::question))
    }

    fun onTabSelected(tab: IoniTab) {
        local.update { it.copy(tab = tab) }
    }

    fun onQuestionClick(index: Int) {
        val entry = entries.getOrNull(index) ?: return
        ask(question = entry.question, answer = entry.answer)
    }

    fun onInputChange(text: String) {
        local.update { it.copy(input = text.take(IONI_INPUT_LIMIT)) }
    }

    fun onSendClick() {
        val state = local.value
        if (!state.isSendEnabled) return
        val question = state.input.trim()
        if (question.isEmpty()) return

        local.update { it.copy(input = "") }
        ask(question = question, answer = faqRepository.stubAnswer())
    }

    fun onSupportClick() {
        local.update { it.copy(isSupportExpanded = !it.isSupportExpanded) }
    }

    private fun ask(question: String, answer: String) {
        answerJob?.cancel()
        answerJob = viewModelScope.launch {
            local.update { it.copy(question = question, answer = "", isThinking = true) }

            delay(THINKING_MILLIS)

            local.update { it.copy(isThinking = false) }

            var shown = 0
            while (shown < answer.length) {
                shown = (shown + typingStep(shown)).coerceAtMost(answer.length)
                local.update { it.copy(answer = answer.take(shown)) }
                delay(TYPING_TICK_MILLIS)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

internal fun typingStep(shown: Int): Int =
    (1 + shown / TYPING_RAMP_CHARS).coerceAtMost(TYPING_MAX_STEP)
