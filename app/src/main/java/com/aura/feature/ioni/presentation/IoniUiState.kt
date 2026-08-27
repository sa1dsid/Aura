package com.aura.feature.ioni.presentation

import androidx.compose.runtime.Immutable

const val IONI_INPUT_LIMIT = 200

enum class IoniTab { ABOUT, AI }

@Immutable
data class IoniUiState(
    val tab: IoniTab = IoniTab.ABOUT,
    val questions: List<String> = emptyList(),
    val question: String? = null,
    val answer: String = "",
    val isThinking: Boolean = false,
    val input: String = "",
    val supportEmail: String = "",
    val isSupportExpanded: Boolean = false,
    val isAiReleased: Boolean = false,
) {

    val isSendEnabled: Boolean get() = input.isNotBlank() && !isThinking

    val hasDialogue: Boolean get() = question != null
}

@Immutable
data class IoniActions(
    val onTabSelected: (IoniTab) -> Unit = {},
    val onQuestionClick: (Int) -> Unit = {},
    val onInputChange: (String) -> Unit = {},
    val onSendClick: () -> Unit = {},
    val onSupportClick: () -> Unit = {},
    val onEmailClick: (String) -> Unit = {},
)
