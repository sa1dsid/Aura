package com.aura.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.AuraPill
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.NodeStatus
import com.aura.feature.home.domain.model.NodeTier
import com.aura.feature.home.presentation.format.displayName
import com.aura.feature.home.presentation.format.formatGrouped
import com.aura.feature.home.presentation.format.formatRate
import com.aura.feature.home.presentation.format.stepLabel

@Composable
fun NodeStatusCard(
    nodeStatus: NodeStatus,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "NODE STATUS",
                    style = AuraTheme.typography.cardLabel,
                    color = colors.textSecondary,
                )
                AuraPill(
                    text = "×${nodeStatus.referralRate.formatRate()} rate",
                    contentColor = colors.iceBlue,
                    borderColor = colors.iceBlue.copy(alpha = 0.60f),
                    backgroundColor = colors.iceBlue.copy(alpha = 0.22f),
                    borderWidth = 0.5.dp,
                    horizontalPadding = 12.dp,
                    verticalPadding = 5.dp,
                )
            }

            Spacer(Modifier.height(18.dp))

            TierLadder(
                currentTier = nodeStatus.currentTier,
                modifier = Modifier.padding(horizontal = 14.dp),
            )

            Spacer(Modifier.height(16.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )

            Spacer(Modifier.height(13.dp))

            TierProgressLine(
                nodeStatus = nodeStatus,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
    }
}

@Composable
private fun TierLadder(
    currentTier: NodeTier,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val tiers = NodeTier.entries

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .drawBehind {
                    val y = size.height / 2f
                    val step = size.width / tiers.size
                    drawLine(
                        color = colors.borderStrong,
                        start = Offset(step / 2f, y),
                        end = Offset(size.width - step / 2f, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tiers.forEach { tier ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    TierDot(isActive = tier == currentTier)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            tiers.forEach { tier ->
                Text(
                    text = tier.stepLabel(),
                    style = AuraTheme.typography.stepLabel,
                    color = if (tier == currentTier) colors.textPrimary else colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TierDot(isActive: Boolean, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    if (isActive) {
        Box(
            modifier = modifier
                .size(16.dp)
                .drawBehind {
                    val radius = size.minDimension / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.green.copy(alpha = 0.50f),
                                colors.green.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                            radius = radius,
                        ),
                        radius = radius,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.green)
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.borderStrong, CircleShape)
        )
    }
}

@Composable
private fun TierProgressLine(
    nodeStatus: NodeStatus,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val target = nodeStatus.progressTarget
    val nextTier = nodeStatus.nextTier

    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = colors.progressValue)) {
            append(nodeStatus.progressToNext.formatGrouped())
        }
        if (target != null) {
            withStyle(SpanStyle(color = colors.progressTarget)) {
                append(" / ${target.formatGrouped()}")
            }
        }
        if (nextTier != null) {
            withStyle(SpanStyle(color = colors.textSecondary)) { append(" to ") }
            withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold)) {
                append(nextTier.displayName())
            }
        }
    }

    Text(
        text = text,
        style = AuraTheme.typography.body,
        modifier = modifier,
    )
}
