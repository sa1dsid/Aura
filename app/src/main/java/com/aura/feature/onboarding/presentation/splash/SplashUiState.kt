package com.aura.feature.onboarding.presentation.splash

import com.aura.feature.onboarding.domain.model.StartDestination

data class SplashUiState(
    val log: String = "",
    val printedLength: Int = 0,
    val startDestination: StartDestination? = null,
)
