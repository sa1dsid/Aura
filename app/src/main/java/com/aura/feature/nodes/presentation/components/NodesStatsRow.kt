package com.aura.feature.nodes.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.R
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.presentation.format.formatGrouped
import com.aura.feature.nodes.domain.model.NodesState

private val CardHeight = 62.dp

@Composable
fun NodesStatsRow(
    state: NodesState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatCard(
            value = state.friendsJoined.formatGrouped(),
            caption = stringResource(R.string.nodes_friends_joined),
            modifier = Modifier.weight(1f),
        )

        val nextTier = state.nextTier
        if (nextTier == null) {
            StatCard(
                value = null,
                caption = stringResource(R.string.nodes_prime_reached),
                modifier = Modifier.weight(1f),
            )
        } else {
            StatCard(
                value = state.friendsToNextTier.formatGrouped(),
                caption = stringResource(R.string.nodes_more_for, stringResource(nextTier.labelRes)),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String?,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier.height(CardHeight)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (value != null) {
                Text(
                    text = value,
                    style = AuraTheme.typography.dataValue,
                    color = colors.textBright,
                    maxLines = 1,
                )
            }
            Text(
                text = caption,
                style = AuraTheme.typography.cardCaption,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
