package com.aura.feature.promo.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.common.formatClock
import com.aura.core.common.formatDayShort
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.promo.domain.model.PromoCodeKind

private val TicketShape = RoundedCornerShape(16.dp)

private val TicketHeight = 64.dp

private val NotchSize = 14.dp

private val DividerHeight = 42.dp

private val DividerWidth = 1.dp

private val DashOn = 3.dp

private val DashOff = 2.dp

private const val USED_ALPHA = 0.72f

@Composable
fun PromoTicket(
    code: PromoCode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val accent = when (code.kind) {
        PromoCodeKind.SPARK -> colors.green
        PromoCodeKind.VPN -> colors.accentBlue
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    val highlight by animateFloatAsState(
        targetValue = if (isPressed) 0.10f else 0f,
        animationSpec = tween(140),
        label = "ticket-highlight",
    )

    Box(
        modifier = modifier
            .pressScale(pressed = isPressed)
            .fillMaxWidth()
            .height(TicketHeight)
            .alpha(if (code.used) USED_ALPHA else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .clip(TicketShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = 0.14f + highlight),
                            accent.copy(alpha = 0.03f + highlight),
                        )
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.40f), TicketShape)
        )

        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painter = painterResource(
                    when (code.kind) {
                        PromoCodeKind.SPARK -> R.drawable.ic_rocket
                        PromoCodeKind.VPN -> R.drawable.ic_shield
                    }
                ),
                contentDescription = null,
                tint = if (code.kind == PromoCodeKind.SPARK) colors.textBright else accent,
                modifier = Modifier.size(24.dp),
            )

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.width(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TicketDivider()
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = code.code,
                        style = AuraTheme.typography.latestValue,
                        color = colors.textBright,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(
                            R.string.promo_ticket_date,
                            code.issuedAt.formatDayShort(),
                            code.issuedAt.formatClock(),
                        ),
                        style = AuraTheme.typography.unitLabel,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }

            KindBadge(code = code, accent = accent)

            CopyButton()
        }

        Notch(Modifier.align(Alignment.CenterStart).offset(x = (-6).dp))
        Notch(Modifier.align(Alignment.CenterEnd).offset(x = 6.dp))
    }
}

@Composable
private fun TicketDivider(modifier: Modifier = Modifier) {
    val color = AuraTheme.colors.textDisabled

    Box(
        modifier
            .width(DividerWidth)
            .requiredHeight(DividerHeight)
            .drawWithCache {
                val effect = PathEffect.dashPathEffect(
                    floatArrayOf(DashOn.toPx(), DashOff.toPx()),
                )
                val x = size.width / 2f

                onDrawBehind {
                    drawLine(
                        color = color,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = size.width,
                        pathEffect = effect,
                    )
                }
            }
    )
}

@Composable
private fun KindBadge(code: PromoCode, accent: Color, modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = when {
                code.used -> stringResource(R.string.badge_used)
                code.kind == PromoCodeKind.SPARK -> stringResource(R.string.promo_badge_spark)
                else -> stringResource(R.string.promo_badge_vpn)
            },
            style = AuraTheme.typography.caption,
            color = if (code.used) colors.textSecondary else accent,
            maxLines = 1,
        )
    }
}

@Composable
private fun CopyButton(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .size(34.dp)
            .clip(shape)
            .background(colors.textBright.copy(alpha = 0.06f))
            .border(1.dp, colors.textBright.copy(alpha = 0.25f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_copy),
            contentDescription = stringResource(R.string.cd_copy_code),
            tint = colors.textBright,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun Notch(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(NotchSize)
            .clip(CircleShape)
            .background(AuraTheme.colors.background)
    )
}
