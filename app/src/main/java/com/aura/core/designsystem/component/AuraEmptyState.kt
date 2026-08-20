package com.aura.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.theme.AuraTheme

private val CircleSize = 64.dp

private val IconSize = 24.dp

@Composable
fun AuraEmptyState(
    @DrawableRes iconRes: Int,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    textWidth: Dp? = null,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(CircleSize)
                .clip(CircleShape)
                .background(colors.glowSky.copy(alpha = 0.12f))
                .border(0.5.dp, colors.textSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(IconSize),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = AuraTheme.typography.sheetHeading,
                color = colors.textBright,
            )
            Text(
                text = text,
                style = AuraTheme.typography.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = if (textWidth == null) Modifier else Modifier.width(textWidth),
            )
        }
    }
}
