package com.aura.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AuraColors(
    val background: Color,
    val backgroundGlow: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val mint: Color,
    val green: Color,
    val iceBlue: Color,
    val progressValue: Color,
    val progressTarget: Color,
    val accentBlue: Color,
    val accentBlueSoft: Color,
    val danger: Color,
    val mapDotIdle: Color,
    val mapDotLive: Color,
    val mapDotUser: Color,
)

val AuraDarkColors = AuraColors(
    background = Color(0xFF05070A),
    backgroundGlow = Color(0xFF0E2233),
    surface = Color(0xFF0A0E13),
    surfaceElevated = Color(0xFF0E141B),
    border = Color(0xFF1A2129),
    borderStrong = Color(0xFF2A343E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8FA7B7),
    textTertiary = Color(0xFF49545D),
    mint = Color(0xFF6FE7CE),
    green = Color(0xFFA7F3D0),
    iceBlue = Color(0xFFA8E6FF),
    progressValue = Color(0xFFA7F3D0),
    progressTarget = Color(0xFFD1C4E9),
    accentBlue = Color(0xFFA8D6F0),
    accentBlueSoft = Color(0xFFD3ECFA),
    danger = Color(0xFFE5484D),
    mapDotIdle = Color(0xFF2E3A45),
    mapDotLive = Color(0xFFFFFFFF),
    mapDotUser = Color(0xFFA8D6F0),
)
