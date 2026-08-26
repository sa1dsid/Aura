package com.aura.feature.ioni.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.aura.core.designsystem.component.auraGlow
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.ioni.presentation.IoniTab

private val BarHeight = 60.dp

private val PlanetBox = 24.dp

private val PlanetIcon = 18.dp

private val TabShape = RoundedCornerShape(16.dp)

private val BalanceWidth = 44.dp

private const val TAB_FADE_MILLIS = 200

private const val FAR_GLOW_ALPHA = 0.40f

private const val NEAR_GLOW_ALPHA = 0.52f

@Composable
fun IoniTopBar(
    selected: IoniTab,
    onTabSelected: (IoniTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Logo()

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabPill(
                text = stringResource(R.string.ioni_tab_about),
                isSelected = selected == IoniTab.ABOUT,
                onClick = { onTabSelected(IoniTab.ABOUT) },
            )
            TabPill(
                text = stringResource(R.string.ioni_tab_ai),
                isSelected = selected == IoniTab.AI,
                onClick = { onTabSelected(IoniTab.AI) },
            )
        }

        Box(Modifier.width(BalanceWidth))
    }
}

@Composable
private fun Logo(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val style = AuraTheme.typography.screenHeading

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.ioni_logo_start),
            style = style,
            color = colors.textBright,
        )

        Box(
            modifier = Modifier
                .size(PlanetBox)
                .auraGlow(
                    color = Color.White.copy(alpha = FAR_GLOW_ALPHA),
                    width = 22.5.dp,
                    height = 22.5.dp,
                    blurRadius = 30.dp,
                )
                .auraGlow(
                    color = Color.White.copy(alpha = NEAR_GLOW_ALPHA),
                    width = 18.dp,
                    height = 18.dp,
                    blurRadius = 12.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_global),
                contentDescription = null,
                tint = colors.textBright,
                modifier = Modifier.size(PlanetIcon),
            )
        }

        Text(
            text = stringResource(R.string.ioni_logo_end),
            style = style,
            color = colors.textBright,
        )
    }
}

@Composable
private fun TabPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    val background by animateColorAsState(
        targetValue = if (isSelected) colors.authSegmentActive else Color.Transparent,
        animationSpec = tween(TAB_FADE_MILLIS),
        label = "ioni-tab-background",
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) colors.textPrimary else colors.authTextMuted,
        animationSpec = tween(TAB_FADE_MILLIS),
        label = "ioni-tab-content",
    )

    Box(
        modifier = Modifier
            .clip(TabShape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 15.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.title,
            color = content,
        )
    }
}
