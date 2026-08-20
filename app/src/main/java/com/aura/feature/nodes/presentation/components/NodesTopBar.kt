package com.aura.feature.nodes.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraBurgerButton
import com.aura.core.designsystem.component.AuraNewsPlanet
import com.aura.core.designsystem.theme.AuraTheme

private val BarHeight = 60.dp

private val BarPadding = 16.dp

@Composable
fun NodesTopBar(
    handle: String,
    hasUnreadNews: Boolean,
    onMenuClick: () -> Unit,
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
        AuraBurgerButton(onClick = onMenuClick)

        Text(
            text = stringResource(R.string.handle_format, handle),
            style = AuraTheme.typography.cardTitle,
            color = AuraTheme.colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )

        AuraNewsPlanet(hasUnread = hasUnreadNews, onClick = onNewsClick)
    }
}
