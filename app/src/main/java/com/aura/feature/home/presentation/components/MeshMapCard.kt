package com.aura.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.core.designsystem.component.AuraCard
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.MeshState
import com.aura.feature.home.domain.model.NodesOnline
import com.aura.feature.home.presentation.components.mesh.MeshMap
import com.aura.feature.home.presentation.components.mesh.MeshMapDefaults
import com.aura.feature.home.presentation.format.formatGrouped

@Composable
fun MeshMapCard(
    mesh: MeshState,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 14.dp)) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.surfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accentBlue)
                )
                Text(
                    text = "MESH ACTIVE",
                    style = AuraTheme.typography.cardLabel,
                    color = colors.textSecondary,
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(Modifier.fillMaxWidth()) {
                MeshMap(
                    cities = mesh.cities,
                    userPresence = mesh.userPresence,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .aspectRatio(MeshMapDefaults.Projection.aspectRatio),
                )

                NodesOnlineCounter(
                    nodesOnline = mesh.nodesOnline,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun NodesOnlineCounter(
    nodesOnline: NodesOnline,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val count = when (nodesOnline) {
        is NodesOnline.Live -> nodesOnline.count
        is NodesOnline.LastKnown -> nodesOnline.count
        NodesOnline.Unknown -> null
    } ?: return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = count.formatGrouped(),
            style = AuraTheme.typography.displayNumber,
            color = colors.textPrimary,
        )
        Text(
            text = "NODES ONLINE",
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
            textAlign = TextAlign.End,
        )
    }
}
