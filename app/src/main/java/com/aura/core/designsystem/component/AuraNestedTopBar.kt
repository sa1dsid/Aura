package com.aura.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.theme.AuraTheme

private val BarHeight = 60.dp

private val BarPadding = 16.dp

@Composable
fun AuraBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.88f)
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_left),
            contentDescription = stringResource(R.string.cd_back),
            tint = AuraTheme.colors.textPrimary,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun AuraNestedTopBar(
    handle: String?,
    hasUnreadNews: Boolean,
    onBackClick: () -> Unit,
    onNewsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .padding(horizontal = BarPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AuraBackButton(onClick = onBackClick)

        Text(
            text = handle?.let { stringResource(R.string.handle_format, it) }.orEmpty(),
            style = AuraTheme.typography.cardTitle,
            color = AuraTheme.colors.textPrimary,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )

        AuraNewsPlanet(hasUnread = hasUnreadNews, onClick = onNewsClick)
    }
}
