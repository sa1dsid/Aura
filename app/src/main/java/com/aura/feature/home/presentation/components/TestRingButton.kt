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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.TestSessionState
import com.aura.feature.home.presentation.format.formatHoursMinutesSeconds
import com.aura.feature.home.presentation.format.formatMinutesSeconds

private val RingDiameter = 226.dp
private val RingStroke = 4.5.dp

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

    val baseGlow = if (isMuted) 0.05f else 0.16f
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed && isReady) baseGlow * 2.2f else baseGlow,
        animationSpec = tween(180),
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
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = isReady,
                    onClick = onClick,
                )
                .ringGraphics(
                    fraction = fraction,
                    trackColor = colors.border,
                    arcColors = if (isMuted) {
                        listOf(
                            colors.accentBlue.copy(alpha = 0.35f),
                            colors.accentBlue.copy(alpha = 0.15f),
                        )
                    } else {
                        listOf(colors.accentBlueSoft, colors.accentBlue)
                    },
                    glowAlpha = glowAlpha,
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
    arcColors: List<Color>,
    glowAlpha: Float,
): Modifier = drawBehind {
    val strokePx = RingStroke.toPx()
    val inset = strokePx * 2.5f
    val arcTopLeft = Offset(inset, inset)
    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)

    drawHalo(glowAlpha, arcColors.first())

    drawArc(
        color = trackColor,
        startAngle = START_ANGLE,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
    )

    val brush = Brush.linearGradient(
        colors = arcColors,
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height),
    )

    GLOW_PASSES.forEach { (widthFactor, alpha) ->
        drawArc(
            brush = brush,
            startAngle = START_ANGLE,
            sweepAngle = 360f * fraction,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokePx * widthFactor, cap = StrokeCap.Round),
            alpha = alpha,
        )
    }
}

private fun DrawScope.drawHalo(glowAlpha: Float, color: Color) {
    val radius = size.minDimension / 2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = glowAlpha),
                color.copy(alpha = glowAlpha * 0.35f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
    )
}

@Composable
private fun RingLabels(session: TestSessionState) {
    val colors = AuraTheme.colors

    val (caption, value, footnote) = when (session) {
        is TestSessionState.Ready ->
            Triple("TAP TO", "START", "+${session.rewardIon} ION")

        is TestSessionState.Running ->
            Triple("TESTING", session.remaining.formatMinutesSeconds(), "+${session.rewardIon} ION")

        is TestSessionState.Cooldown -> Triple(
            "NEXT TEST IN",
            session.remaining.formatHoursMinutesSeconds(),
            if (session.isPausedByVpn) "PAUSED · VPN ON" else "",
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = caption,
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = AuraTheme.typography.timer,
            color = colors.textPrimary,
        )
        if (footnote.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = footnote,
                style = AuraTheme.typography.caption,
                color = colors.textSecondary,
            )
        }
    }
}

private const val START_ANGLE = -90f

private val GLOW_PASSES = listOf(
    5f to 0.07f,
    2.6f to 0.16f,
    1f to 1f,
)
