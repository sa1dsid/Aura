package com.aura.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AuraFontFamily = FontFamily.Monospace

@Immutable
data class AuraTypography(
    val logo: TextStyle,
    val cardLabel: TextStyle,
    val displayNumber: TextStyle,
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
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.1.sp,
    ),
    displayNumber = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp,
    ),
    timer = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 33.sp,
        lineHeight = 38.sp,
        letterSpacing = 1.sp,
    ),
    title = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp,
    ),
    body = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    caption = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp,
    ),
    badge = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
    navLabel = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.sp,
    ),
    stepLabel = TextStyle(
        fontFamily = AuraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        letterSpacing = 0.2.sp,
    ),
)
