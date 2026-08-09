package com.aura.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aura.R

private val AuraFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

private val AuraDisplayFamily = FontFamily(
    Font(R.font.space_grotesk_light, FontWeight.Light),
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

@Immutable
data class AuraTypography(
    val logo: TextStyle,
    val cardLabel: TextStyle,
    val displayNumber: TextStyle,
    val counterNumber: TextStyle,
    val unitLabel: TextStyle,
    val timer: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val badge: TextStyle,
    val navLabel: TextStyle,
    val stepLabel: TextStyle,
)

val AuraDefaultTypography = AuraTypography(
    logo = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = 1.4.sp,
    ),
    cardLabel = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.2.sp,
        letterSpacing = 0.12.sp,
    ),
    displayNumber = TextStyle(
        fontFamily = AuraDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.6.sp,
        letterSpacing = 0.sp,
    ),
    counterNumber = TextStyle(
        fontFamily = AuraDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.4.sp,
        letterSpacing = 0.sp,
    ),
    unitLabel = TextStyle(
        fontFamily = AuraDisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.8.sp,
        letterSpacing = 0.sp,
    ),
    timer = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 29.sp,
        letterSpacing = 0.22.sp,
    ),
    title = TextStyle(
        fontFamily = AuraDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.3.sp,
        letterSpacing = 0.sp,
    ),
    body = TextStyle(
        fontFamily = AuraDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.3.sp,
        letterSpacing = 0.sp,
    ),
    caption = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.2.sp,
        letterSpacing = 0.sp,
    ),
    badge = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.8.sp,
        letterSpacing = 0.sp,
    ),
    navLabel = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.2.sp,
        letterSpacing = 0.sp,
    ),
    stepLabel = TextStyle(
        fontFamily = AuraDisplayFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.8.sp,
        letterSpacing = 0.sp,
    ),
)
