package com.aura.feature.onboarding.presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MIN_GAP_ABOVE_NAVIGATION_BAR = 12.dp

@Composable
fun designBottomGap(designGap: Dp): Dp {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return maxOf(designGap, navigationBar + MIN_GAP_ABOVE_NAVIGATION_BAR)
}
