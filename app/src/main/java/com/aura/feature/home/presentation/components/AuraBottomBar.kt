package com.aura.feature.home.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.R
import com.aura.core.designsystem.component.auraBlurRadius
import com.aura.core.designsystem.component.auraGlow
import com.aura.core.designsystem.component.pressScale
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.presentation.HomeTab

private const val TAB_FADE_MILLIS = 200

private const val ICON_GLOW_ALPHA = 0.20f

private val ICON_SIZE = 18.dp

private val ICON_GLOW_SIZE = 19.5.dp

private val ICON_GLOW_BLUR = 6.dp

@Composable
fun AuraBottomBar(
    selected: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TabItem(
                tab = HomeTab.HOME,
                iconRes = R.drawable.ic_home,
                isSelected = selected == HomeTab.HOME,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
            TabItem(
                tab = HomeTab.NODES,
                iconRes = R.drawable.ic_users,
                isSelected = selected == HomeTab.NODES,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
            IoniTab(
                isSelected = selected == HomeTab.IONI,
                onClick = { onTabSelected(HomeTab.IONI) },
                modifier = Modifier.weight(1f),
            )
            TabItem(
                tab = HomeTab.TERMINAL,
                iconRes = R.drawable.ic_wallet,
                isSelected = selected == HomeTab.TERMINAL,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
            TabItem(
                tab = HomeTab.NETWORK,
                iconRes = R.drawable.ic_wifi,
                isSelected = selected == HomeTab.NETWORK,
                onClick = onTabSelected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: HomeTab,
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    onClick: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val iconGlowBlur = remember(density) { with(density) { auraBlurRadius(ICON_GLOW_BLUR) } }

    val tint by animateColorAsState(
        targetValue = if (isSelected) colors.textBright else colors.textSecondary,
        animationSpec = tween(TAB_FADE_MILLIS),
        label = "tab-tint",
    )
    val glowAlpha = animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(TAB_FADE_MILLIS),
        label = "tab-glow",
    )
    val hasGlow by remember { derivedStateOf { glowAlpha.value > 0f } }

    val glowModifier = remember(colors.tabGlow) {
        Modifier
            .auraGlow(
                color = colors.tabGlow.copy(alpha = 0.10f),
                width = 31.dp,
                height = 55.dp,
                blurRadius = 40.dp,
                alpha = { glowAlpha.value },
            )
            .auraGlow(
                color = Color.White.copy(alpha = 0.12f),
                width = 24.dp,
                height = 31.dp,
                blurRadius = 10.dp,
                alpha = { glowAlpha.value },
            )
    }

    Column(
        modifier = modifier
            .height(56.dp)
            .pressScale(interactionSource, pressedScale = 0.9f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onClick(tab) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .then(glowModifier),
            contentAlignment = Alignment.Center,
        ) {
            if (hasGlow) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = ICON_GLOW_ALPHA),
                    modifier = Modifier
                        .size(ICON_GLOW_SIZE)
                        .graphicsLayer { alpha = glowAlpha.value }
                        .blur(iconGlowBlur, BlurredEdgeTreatment.Unbounded),
                )
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(tab.labelRes),
                tint = tint,
                modifier = Modifier.size(ICON_SIZE),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = AuraTheme.typography.navLabel,
            color = tint,
        )
    }
}

@Composable
private fun IoniTab(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    val borderColor by animateColorAsState(
        targetValue = colors.iceBlue.copy(alpha = if (isSelected) 0.80f else 0.60f),
        animationSpec = tween(TAB_FADE_MILLIS),
        label = "ioni-border",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .pressScale(interactionSource, pressedScale = 0.92f)
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.iceBlue.copy(alpha = 0.12f))
                .border(
                    width = 0.5.dp,
                    color = borderColor,
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(HomeTab.IONI.labelRes),
                style = AuraTheme.typography.badge.copy(
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                ),
                color = colors.textBright,
            )
        }
    }
}
