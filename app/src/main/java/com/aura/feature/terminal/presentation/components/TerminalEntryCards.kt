package com.aura.feature.terminal.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.AuraShadow
import com.aura.core.designsystem.component.auraGlowLayers
import com.aura.core.designsystem.theme.AuraColors
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.terminal.domain.model.TerminalCounters

private const val PULSE_MILLIS = 1400

private const val PULSE_FLOOR = 0.35f

private val CardHeight = 93.dp

private val CardBorder = 1.dp

private val CardGap = 16.dp

private val DotSize = 6.dp

private val AuraColors.unreadDotShadows: List<AuraShadow>
    get() = listOf(
        AuraShadow(warning.copy(alpha = 0.29f), 6.dp, 6.dp),
        AuraShadow(warning, 4.dp),
        AuraShadow(warning.copy(alpha = 0.04f), 8.dp, 1.dp),
    )

@Composable
fun TerminalEntryCards(
    counters: TerminalCounters,
    onTransactionsClick: () -> Unit,
    onPromoCodesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        TerminalEntryCard(
            label = stringResource(R.string.terminal_card_transactions),
            count = counters.unreadTransactions,
            hint = stringResource(R.string.terminal_card_transactions_hint),
            onClick = onTransactionsClick,
            modifier = Modifier.weight(1f),
        )
        TerminalEntryCard(
            label = stringResource(R.string.terminal_card_promo),
            count = counters.unreadPromoCodes,
            hint = stringResource(R.string.terminal_card_promo_hint),
            onClick = onPromoCodesClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TerminalEntryCard(
    label: String,
    count: Int,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(
        modifier = modifier.height(CardHeight),
        onClick = onClick,
        glowOnPress = true,
        containerColor = colors.background,
        borderWidth = CardBorder,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (count > 0) UnreadDot()

                Text(
                    text = label,
                    style = AuraTheme.typography.cardLabel,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (count > 0) {
                    Text(
                        text = stringResource(R.string.terminal_card_counter, count),
                        style = AuraTheme.typography.actionLabel,
                        color = colors.textIce,
                        maxLines = 1,
                    )
                }

                Text(
                    text = hint,
                    style = AuraTheme.typography.unitLabel,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun UnreadDot(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    val pulse = rememberInfiniteTransition(label = "unread-dot").animateFloat(
        initialValue = PULSE_FLOOR,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "unread-pulse",
    )

    Box(modifier = modifier.size(DotSize)) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = pulse.value }
                .auraGlowLayers(colors.unreadDotShadows)
        )
        Box(
            Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(colors.warning)
        )
    }
}
