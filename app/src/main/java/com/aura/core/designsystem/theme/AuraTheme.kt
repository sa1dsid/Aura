package com.aura.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LocalAuraColors: ProvidableCompositionLocal<AuraColors> =
    staticCompositionLocalOf { AuraDarkColors }

private val LocalAuraTypography: ProvidableCompositionLocal<AuraTypography> =
    staticCompositionLocalOf { AuraDefaultTypography }

object AuraTheme {
    val colors: AuraColors
        @Composable @ReadOnlyComposable get() = LocalAuraColors.current

    val typography: AuraTypography
        @Composable @ReadOnlyComposable get() = LocalAuraTypography.current
}

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    val colors = AuraDarkColors
    val typography = AuraDefaultTypography

    CompositionLocalProvider(
        LocalAuraColors provides colors,
        LocalAuraTypography provides typography,
        LocalTextStyle provides typography.body.copy(color = colors.textPrimary),
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = colors.accentBlue,
                background = colors.background,
                surface = colors.surface,
                onBackground = colors.textPrimary,
                onSurface = colors.textPrimary,
            ),
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(22.dp),
            ),
            content = content,
        )
    }
}
