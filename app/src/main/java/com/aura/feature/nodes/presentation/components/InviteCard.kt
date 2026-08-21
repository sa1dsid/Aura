package com.aura.feature.nodes.presentation.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.component.PRESS_FADE_MILLIS
import com.aura.core.designsystem.component.auraDropShadow
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraColors
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.nodes.domain.model.InviteOffer
import com.aura.feature.nodes.presentation.format.NodesItalic
import com.aura.feature.nodes.presentation.format.figmaBlur
import com.aura.feature.nodes.presentation.format.textGlow

private val CardPadding = 16.dp

private val FieldShape = RoundedCornerShape(16.dp)

private val FieldHeight = 56.dp

private val CopyButtonSize = 32.dp

private val CopyIconSize = 18.dp

private val ShareHeight = 47.dp

private val ShareIconSize = 18.dp

private val QuoteLineHeight = 15.sp

private val CodeGlowBlur = 6.dp

private val CardGlowBlur = 8.dp.figmaBlur()

@Composable
fun InviteCard(
    invite: InviteOffer,
    onCodeClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val glow = rememberCardGlow(colors)

    AuraCard(modifier = modifier.fillMaxWidth().then(glow)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.nodes_invite_title),
                        style = AuraTheme.typography.sheetHeading,
                        color = colors.textBright,
                    )
                    Text(
                        text = invite.quote ?: stringResource(R.string.nodes_invite_quote),
                        style = AuraTheme.typography.body.copy(
                            lineHeight = QuoteLineHeight,
                            textGeometricTransform = NodesItalic,
                        ),
                        color = colors.textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                CodeField(code = invite.code, onClick = onCodeClick)
            }

            ShareButton(onClick = onShareClick)
        }
    }
}

@Composable
private fun rememberCardGlow(colors: AuraColors): Modifier = remember(colors) {
    Modifier.auraDropShadow(
        color = colors.glowIce.copy(alpha = 0.60f),
        blurRadius = CardGlowBlur,
        cornerRadius = 16.dp,
    )
}

@Composable
private fun CodeField(
    code: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    val background by animateColorAsState(
        targetValue = colors.bonusBadgeBorder.copy(alpha = if (isPressed) 0.20f else 0.12f),
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "code-field-background",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FieldHeight)
            .clip(FieldShape)
            .background(background)
            .border(1.dp, colors.border, FieldShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.nodes_code_label),
                style = AuraTheme.typography.cardCaption,
                color = colors.textSecondary,
            )
            Text(
                text = code,
                style = AuraTheme.typography.sheetHeading.copy(
                    shadow = textGlow(colors.glowCyan.copy(alpha = 0.50f), CodeGlowBlur),
                ),
                color = colors.accentBlue,
            )
        }

        Box(
            modifier = Modifier
                .size(CopyButtonSize)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.textBright.copy(alpha = 0.06f))
                .border(1.dp, colors.textBright.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_copy_square),
                contentDescription = stringResource(R.string.cd_copy_code),
                tint = colors.textBright,
                modifier = Modifier.size(CopyIconSize),
            )
        }
    }
}

@Composable
private fun ShareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by rememberPressedState(interactionSource)

    val background by animateColorAsState(
        targetValue = colors.iceBlue.copy(alpha = if (isPressed) 0.32f else 0.22f),
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "share-button-background",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ShareHeight)
            .pressScale(pressed = isPressed)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, colors.iceBlue.copy(alpha = 0.60f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_share_nodes),
            contentDescription = null,
            tint = colors.textBright,
            modifier = Modifier.size(ShareIconSize),
        )
        Text(
            text = stringResource(R.string.nodes_share),
            style = AuraTheme.typography.sheetHeading,
            color = colors.textBright,
        )
    }
}
