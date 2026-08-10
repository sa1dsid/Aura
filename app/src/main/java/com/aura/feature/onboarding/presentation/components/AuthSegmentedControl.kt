package com.aura.feature.onboarding.presentation.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.onboarding.domain.model.AuthMode

private val ContainerShape = RoundedCornerShape(14.dp)

private val ThumbShape = RoundedCornerShape(10.dp)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AuthSegmentedControl(
    mode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(47.dp)
            .clip(ContainerShape)
            .background(colors.authBackground)
            .border(1.dp, colors.authBorderSoft, ContainerShape)
            .padding(horizontal = 7.dp, vertical = 5.dp),
    ) {
        val thumbWidth = maxWidth / 2
        val thumbOffset by animateFloatAsState(
            targetValue = if (mode == AuthMode.SIGN_IN) 0f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "segment-thumb",
        )

        Box(
            modifier = Modifier
                .offset(x = thumbWidth * thumbOffset)
                .width(thumbWidth)
                .fillMaxHeight()
                .clip(ThumbShape)
                .background(colors.authSegmentActive)
                .border(1.dp, colors.authBorderSoft, ThumbShape)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            SegmentLabel(
                text = "Sign In",
                selected = mode == AuthMode.SIGN_IN,
                onClick = { onModeChange(AuthMode.SIGN_IN) },
                modifier = Modifier.width(thumbWidth),
            )
            SegmentLabel(
                text = "Sign Up",
                selected = mode == AuthMode.SIGN_UP,
                onClick = { onModeChange(AuthMode.SIGN_UP) },
                modifier = Modifier.width(thumbWidth),
            )
        }
    }
}

@Composable
private fun SegmentLabel(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.authSegmentActiveText else colors.authTextMuted,
        animationSpec = tween(160),
        label = "segment-label",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(ThumbShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.segmentLabel,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}
