package com.aura.feature.ioni.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class FaqEntry(
    val question: String,
    val answer: String,
)
