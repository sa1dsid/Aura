package com.aura.feature.nodes.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.PRESS_FADE_MILLIS
import com.aura.core.designsystem.component.auraDropShadows
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.component.rememberPressedState
import com.aura.core.designsystem.theme.AuraColors
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.nodes.domain.model.SocialLink
import com.aura.feature.nodes.domain.model.SocialNetwork
import com.aura.feature.nodes.presentation.format.socialPressShadows

private val RowShape = RoundedCornerShape(16.dp)

private val RowHeight = 64.dp

private val LogoSize = 32.dp

private val ArrowSize = 24.dp

@Composable
fun SocialSection(
    socials: List<SocialLink>,
    onSocialClick: (SocialLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.nodes_find_title),
                style = AuraTheme.typography.sectionTitle,
                color = colors.textSecondary,
            )
            Text(
                text = stringResource(R.string.nodes_find_subtitle),
                style = AuraTheme.typography.body,
                color = colors.textSecondary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            socials.forEach { link ->
                SocialRow(link = link, onClick = { onSocialClick(link) })
            }
        }
    }
}

@Composable
private fun SocialRow(
    link: SocialLink,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = rememberPressedState(interactionSource)

    val glowAlpha = animateFloatAsState(
        targetValue = if (pressed.value) 1f else 0f,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "social-glow",
    )
    val borderColor by animateColorAsState(
        targetValue = if (pressed.value) colors.accentBlue else colors.border,
        animationSpec = tween(PRESS_FADE_MILLIS),
        label = "social-border",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .pressScale(pressed = pressed.value)
            .then(rememberSocialGlow(colors, glowAlpha))
            .clip(RowShape)
            .background(colors.surfaceTop)
            .border(0.5.dp, borderColor, RowShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Bracket(text = stringResource(R.string.nodes_bracket_open))
            Image(
                painter = painterResource(link.network.logoRes),
                contentDescription = null,
                modifier = Modifier.size(LogoSize),
            )
            Bracket(text = stringResource(R.string.nodes_bracket_close))
        }

        Text(
            text = stringResource(link.network.labelRes),
            style = AuraTheme.typography.listRowTitle,
            color = colors.textBright,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )

        Icon(
            painter = painterResource(R.drawable.ic_export),
            contentDescription = stringResource(R.string.cd_open_link),
            tint = colors.bonusBadgeBorder,
            modifier = Modifier.size(ArrowSize),
        )
    }
}

@Composable
private fun Bracket(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AuraTheme.typography.bracket,
        color = Color.White.copy(alpha = 0.40f),
        modifier = modifier,
    )
}

@Composable
private fun rememberSocialGlow(colors: AuraColors, alpha: State<Float>): Modifier =
    remember(colors, alpha) {
        Modifier.auraDropShadows(
            shadows = colors.socialPressShadows,
            cornerRadius = 16.dp,
            alpha = alpha::value,
        )
    }

private val SocialNetwork.logoRes: Int
    get() = when (this) {
        SocialNetwork.DISCORD -> R.drawable.ic_social_discord
        SocialNetwork.TELEGRAM -> R.drawable.ic_social_telegram
        SocialNetwork.X -> R.drawable.ic_social_x
        SocialNetwork.REDDIT -> R.drawable.ic_social_reddit
        SocialNetwork.INSTAGRAM -> R.drawable.ic_social_instagram
        SocialNetwork.SNAPCHAT -> R.drawable.ic_social_snapchat
    }

private val SocialNetwork.labelRes: Int
    get() = when (this) {
        SocialNetwork.DISCORD -> R.string.social_discord
        SocialNetwork.TELEGRAM -> R.string.social_telegram
        SocialNetwork.X -> R.string.social_x
        SocialNetwork.REDDIT -> R.string.social_reddit
        SocialNetwork.INSTAGRAM -> R.string.social_instagram
        SocialNetwork.SNAPCHAT -> R.string.social_snapchat
    }
