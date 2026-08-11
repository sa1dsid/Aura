package com.aura.feature.home.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraShadow
import com.aura.core.designsystem.component.auraArcGlow
import com.aura.core.designsystem.component.auraGlowLayer
import com.aura.core.designsystem.component.drawAuraArcGlow
import com.aura.core.designsystem.component.drawAuraGlow
import com.aura.core.designsystem.component.planetCoreShadows
import com.aura.core.designsystem.component.planetRingShadows
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.presentation.format.formatHoursMinutesSeconds
import com.aura.feature.home.presentation.format.formatMinutesSeconds

private val RingDiameter = 162.dp
private val RingStroke = 3.24.dp
private val CoreDiameter = 122.dp
private val CoreBorder = 1.dp

@Composable
fun TestRingButton(
    session: TestSessionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    val isReady = session is TestSessionState.Ready
    val isMuted = session is TestSessionState.Cooldown

    val fraction = when (session) {
        is TestSessionState.Ready -> 1f
        is TestSessionState.Running -> 1f - session.progress
        is TestSessionState.Cooldown -> session.remainingFraction()
    }

    val glowAlpha = animateFloatAsState(
        targetValue = if (isMuted) 0.35f else 1f,
        animationSpec = tween(220),
        label = "ring-glow",
    )

    Box(
        modifier = modifier.size(RingDiameter),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .pressScale(pressed = isPressed, enabled = isReady, pressedScale = 0.965f)
                .fillMaxSize()
                .ringGraphics(
                    fraction = fraction,
                    trackColor = colors.border,
                    arcColor = if (isMuted) colors.accentBlue.copy(alpha = 0.45f) else colors.accentBlue,
                    ringShadows = colors.planetRingShadows,
                    coreShadows = colors.planetCoreShadows,
                    coreColor = colors.planetCore,
                    coreEdgeColor = colors.surfaceBottom,
                    coreBorderColor = colors.accentBlueSoft.copy(alpha = 0.22f),
                    coreInnerGlowColor = colors.accentBlue,
                    glowAlpha = { glowAlpha.value },
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = isReady,
                    onClick = onClick,
                )
        )

        RingLabels(session = session)
    }
}

private fun TestSessionState.Cooldown.remainingFraction(): Float =
    if (total.inWholeMilliseconds == 0L) 0f
    else (remaining.inWholeMilliseconds.toFloat() / total.inWholeMilliseconds).coerceIn(0f, 1f)

private fun Modifier.ringGraphics(
    fraction: Float,
    trackColor: Color,
    arcColor: Color,
    ringShadows: List<AuraShadow>,
    coreShadows: List<AuraShadow>,
    coreColor: Color,
    coreEdgeColor: Color,
    coreBorderColor: Color,
    coreInnerGlowColor: Color,
    glowAlpha: () -> Float,
): Modifier = drawWithCache {
    val strokePx = RingStroke.toPx()
    val arcTopLeft = Offset(strokePx / 2f, strokePx / 2f)
    val arcSize = Size(size.width - strokePx, size.height - strokePx)
    val sweepAngle = 360f * fraction

    val corePx = CoreDiameter.toPx()
    val coreCenter = Offset(size.width / 2f, size.height / 2f)
    val ringGlows = ringShadows.map { auraArcGlow(it, strokePx) }
    val coreGlows = coreShadows.map { auraGlowLayer(it, corePx, corePx, coreCenter) }

    onDrawBehind {
        drawArc(
            color = trackColor,
            startAngle = START_ANGLE,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokePx),
        )

        val alpha = glowAlpha()
        ringGlows.forEach { glow ->
            drawAuraArcGlow(glow, arcTopLeft, arcSize, START_ANGLE, sweepAngle, alpha)
        }

        drawArc(
            color = arcColor,
            startAngle = START_ANGLE,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )

        coreGlows.forEach { drawAuraGlow(it) }

        drawPlanetCore(
            radius = corePx / 2f,
            coreColor = coreColor,
            edgeColor = coreEdgeColor,
            borderColor = coreBorderColor,
            innerGlowColor = coreInnerGlowColor,
            borderWidth = CoreBorder.toPx(),
        )
    }
}

private fun DrawScope.drawPlanetCore(
    radius: Float,
    coreColor: Color,
    edgeColor: Color,
    borderColor: Color,
    innerGlowColor: Color,
    borderWidth: Float,
) {
    if (radius <= 0f) return

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(coreColor, edgeColor),
            center = Offset(center.x, center.y - radius * CORE_LIGHT_SHIFT),
            radius = radius * CORE_LIGHT_RADIUS,
        ),
        radius = radius,
        center = center,
    )

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.45f to Color.Transparent,
                0.78f to innerGlowColor.copy(alpha = 0.03f),
                1f to innerGlowColor.copy(alpha = 0.12f),
            ),
            center = Offset(center.x, center.y + radius * CORE_INNER_SHIFT),
            radius = radius,
        ),
        radius = radius,
        center = center,
    )

    drawCircle(
        color = borderColor,
        radius = radius - borderWidth / 2f,
        center = center,
        style = Stroke(width = borderWidth),
    )
}

@Composable
private fun RingLabels(session: TestSessionState) {
    val colors = AuraTheme.colors

    val (caption, value, footnote) = when (session) {
        is TestSessionState.Ready -> Triple(
            stringResource(R.string.timer_tap_to),
            stringResource(R.string.timer_start),
            stringResource(R.string.timer_reward, session.rewardIon),
        )

        is TestSessionState.Running -> Triple(
            stringResource(R.string.timer_testing),
            session.remaining.formatMinutesSeconds(),
            stringResource(R.string.timer_reward, session.rewardIon),
        )

        is TestSessionState.Cooldown -> Triple(
            stringResource(R.string.timer_next_in),
            session.remaining.formatHoursMinutesSeconds(),
            if (session.isPausedByVpn) stringResource(R.string.timer_paused_vpn) else "",
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = caption,
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = AuraTheme.typography.timer,
            color = colors.textBright,
        )
        if (footnote.isNotEmpty()) {
            Text(
                text = footnote,
                style = AuraTheme.typography.title,
                color = colors.glowSky,
            )
        }
    }
}

private const val START_ANGLE = -90f

private const val CORE_LIGHT_SHIFT = 0.164f

private const val CORE_LIGHT_RADIUS = 1.164f

private const val CORE_INNER_SHIFT = 0.05f
