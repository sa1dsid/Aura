package com.aura.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.presentation.components.AuraBottomBar
import com.aura.feature.home.presentation.components.BalanceCardsRow
import com.aura.feature.home.presentation.components.ConnectionBadge
import com.aura.feature.home.presentation.components.HomeTopBar
import com.aura.feature.home.presentation.components.InviteRow
import com.aura.feature.home.presentation.components.MeshMapCard
import com.aura.feature.home.presentation.components.NodeStatusCard
import com.aura.feature.home.presentation.components.TeaserCards
import com.aura.feature.home.presentation.components.TestRingButton
import com.aura.feature.home.presentation.preview.HomePreviewData

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenResumed()
    }

    HomeScreen(
        uiState = uiState,
        actions = HomeActions(
            onMainButtonClick = viewModel::onMainButtonClick,
        ),
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        bottomBar = {
            AuraBottomBar(
                selected = HomeTab.HOME,
                onTabSelected = actions.onTabSelected,
            )
        },
    ) { innerPadding ->
        when (uiState) {
            HomeUiState.Loading -> LoadingState(Modifier.padding(innerPadding))
            is HomeUiState.Content -> HomeContent(
                state = uiState,
                actions = actions,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    actions: HomeActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val home = state.home

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        HomeTopBar(
            hasUnreadNews = home.news.hasUnread,
            onMenuClick = actions.onMenuClick,
            onNewsClick = actions.onNewsClick,
        )

        Spacer(Modifier.height(10.dp))

        MeshMapCard(mesh = state.mesh)

        Spacer(Modifier.height(10.dp))

        BalanceCardsRow(balances = home.balances)

        Spacer(Modifier.height(10.dp))

        NodeStatusCard(nodeStatus = home.nodeStatus)

        Spacer(Modifier.height(10.dp))

        TeaserCards(
            teasers = home.teasers,
            onBonusWithdrawalClick = actions.onBonusWithdrawalClick,
            onSparkClick = actions.onSparkClick,
            onVpnCodeClick = actions.onVpnCodeClick,
        )

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val center = Offset(size.width / 2f, size.height * 0.46f)
                    val radius = size.width * 0.72f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.backgroundGlow.copy(alpha = 0.45f),
                                colors.backgroundGlow.copy(alpha = 0.14f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConnectionBadge(
                connection = home.connection,
                onClick = actions.onConnectionBadgeClick,
            )

            Spacer(Modifier.height(18.dp))

            TestRingButton(
                session = home.session,
                onClick = actions.onMainButtonClick,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Stay on this screen to complete the test",
                style = AuraTheme.typography.caption,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(16.dp))

        InviteRow(
            invite = home.invite,
            onInviteClick = actions.onInviteClick,
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "CONNECTING TO MESH",
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
        )
    }
}

@Preview(widthDp = 375, heightDp = 1250, showBackground = true, backgroundColor = 0xFF05070A)
@Composable
private fun HomeScreenPreview() {
    AuraTheme {
        HomeScreen(
            uiState = HomePreviewData.content,
            actions = HomeActions(),
        )
    }
}
