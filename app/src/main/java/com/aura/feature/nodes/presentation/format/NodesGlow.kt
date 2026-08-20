package com.aura.feature.nodes.presentation.format

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.AuraShadow
import com.aura.core.designsystem.theme.AuraColors

private const val FIGMA_BLUR_SCALE = 0.80f

fun Dp.figmaBlur(): Dp = this * FIGMA_BLUR_SCALE

val AuraColors.squadShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(green, 1.dp.figmaBlur()),
        AuraShadow(green, 4.dp.figmaBlur()),
        AuraShadow(Color.White.copy(alpha = 0.20f), 8.dp.figmaBlur(), 1.dp),
    )

val AuraColors.socialPressShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(accentBlue.copy(alpha = 0.20f), 3.dp.figmaBlur()),
        AuraShadow(glowCyan.copy(alpha = 0.10f), 6.dp.figmaBlur(), 1.dp),
        AuraShadow(Color.White.copy(alpha = 0.30f), 20.dp.figmaBlur(), 3.dp),
    )
