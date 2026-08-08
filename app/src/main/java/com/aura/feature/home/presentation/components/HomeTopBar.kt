package com.aura.feature.home.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.drawPlanet
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.theme.AuraTheme

@Composable
fun HomeTopBar(
    hasUnreadNews: Boolean,
    onMenuClick: () -> Unit,
    onNewsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BurgerButton(onClick = onMenuClick)

        Text(
            text = "I Ø Aura",
            style = AuraTheme.typography.logo,
            color = colors.textPrimary,
        )

        NewsPlanet(hasUnread = hasUnreadNews, onClick = onNewsClick)
    }
}

@Composable
private fun BurgerButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = AuraTheme.colors.textPrimary
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.88f)
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_menu),
            contentDescription = "Menu",
            tint = color,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun NewsPlanet(
    hasUnread: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    val pulse = rememberInfiniteTransition(label = "news-planet").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "news-pulse",
    )

    Box(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.9f)
            .size(44.dp)
            .drawBehind { drawPlanet(bodyColor = colors.background, dotColor = colors.mapDotIdle) }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (hasUnread) {
            Box(
                Modifier
                    .size(34.dp)
                    .drawBehind {
                        val radius = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    colors.danger.copy(alpha = 0.55f * pulse.value),
                                ),
                                radius = radius,
                            ),
                            radius = radius,
                        )
                    }
            )
        }
    }
}
