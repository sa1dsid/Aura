package com.aura.feature.home.presentation

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.R
import com.aura.core.designsystem.component.AuraToastHost
import com.aura.core.designsystem.component.AuraToastKind
import com.aura.core.designsystem.component.AuraToastState
import com.aura.core.designsystem.component.rememberAuraToastState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.domain.model.TestStartRejection
import com.aura.feature.home.presentation.components.AuraBottomBar
import com.aura.feature.home.presentation.components.BalanceCardsRow
import com.aura.feature.home.presentation.components.ConnectionBadge
import com.aura.feature.home.presentation.components.HomeTopBar
import com.aura.feature.home.presentation.components.InviteRow
import com.aura.feature.home.presentation.components.MeshMapCard
import com.aura.feature.home.presentation.components.NodeStatusCard
import com.aura.feature.home.presentation.components.TeaserCards
import com.aura.feature.home.presentation.components.TestRingButton
import com.aura.feature.home.presentation.format.formatHoursMinutes
import com.aura.feature.home.presentation.preview.HomePreviewData

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onTabSelected: (HomeTab) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastState = rememberAuraToastState()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenResumed()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onScreenLeft()
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onScreenLeft() }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            toastState.show(
                text = context.toastText(event),
                kind = event.toastKind(),
            )
        }
    }

    HomeScreen(
        uiState = uiState,
        actions = HomeActions(
            onMenuClick = onMenuClick,
            onMainButtonClick = viewModel::onMainButtonClick,
            onTabSelected = onTabSelected,
        ),
        toastState = toastState,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    toastState: AuraToastState = rememberAuraToastState(),
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
        Box(Modifier.fillMaxSize()) {
            when (uiState) {
                HomeUiState.Loading -> LoadingState(Modifier.padding(innerPadding))
                is HomeUiState.Content -> HomeContent(
                    state = uiState,
                    actions = actions,
                    contentPadding = innerPadding,
                )
            }

            AuraToastHost(
                state = toastState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item {
            HomeTopBar(
                hasUnreadNews = home.news.hasUnread,
                onMenuClick = actions.onMenuClick,
                onNewsClick = actions.onNewsClick,
            )

            Spacer(Modifier.height(10.dp))

            MeshMapCard(mesh = state.mesh)

            Spacer(Modifier.height(10.dp))
        }

        item {
            BalanceCardsRow(balances = home.balances)

            Spacer(Modifier.height(10.dp))

            NodeStatusCard(nodeStatus = home.nodeStatus)

            Spacer(Modifier.height(10.dp))
        }

        item {
            TeaserCards(
                teasers = home.teasers,
                currentTier = home.nodeStatus.currentTier,
                onBonusWithdrawalClick = actions.onBonusWithdrawalClick,
                onSparkClick = actions.onSparkClick,
                onVpnCodeClick = actions.onVpnCodeClick,
            )

            Spacer(Modifier.height(20.dp))
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithCache {
                        val center = Offset(size.width / 2f, size.height * 0.46f)
                        val radius = size.width * 0.72f
                        val brush = Brush.radialGradient(
                            colors = listOf(
                                colors.backgroundGlow.copy(alpha = 0.45f),
                                colors.backgroundGlow.copy(alpha = 0.14f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = radius,
                        )

                        onDrawBehind {
                            drawCircle(brush = brush, radius = radius, center = center)
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ConnectionBadge(
                    connection = home.connection,
                    onClick = actions.onConnectionBadgeClick,
                )

                Spacer(Modifier.height(21.dp))

                TestRingButton(
                    session = home.session,
                    onClick = actions.onMainButtonClick,
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.timer_stay_hint),
                    style = AuraTheme.typography.caption,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        item {
            InviteRow(
                invite = home.invite,
                onInviteClick = actions.onInviteClick,
            )

            Spacer(Modifier.height(16.dp))
        }
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
            text = stringResource(R.string.home_connecting),
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
        )
    }
}

private fun Context.toastText(event: HomeEvent): String = when (event) {
    is HomeEvent.TestRejected -> rejectionText(event.rejection)
    is HomeEvent.TestCompleted -> getString(R.string.toast_session_done, event.rewardIon)
    HomeEvent.TestInterrupted -> getString(R.string.toast_session_interrupted)
    HomeEvent.CooldownResumed -> getString(R.string.toast_vpn_resumed)
}

private fun HomeEvent.toastKind(): AuraToastKind = when (this) {
    is HomeEvent.TestCompleted, HomeEvent.CooldownResumed -> AuraToastKind.SUCCESS
    else -> AuraToastKind.ERROR
}

private fun Context.rejectionText(rejection: TestStartRejection): String = when (rejection) {
    TestStartRejection.DataShareDisabled -> getString(R.string.toast_datashare_off)
    TestStartRejection.VpnDetected -> getString(R.string.toast_vpn_block)
    is TestStartRejection.CooldownNotFinished ->
        getString(R.string.toast_cooldown, rejection.remaining.formatHoursMinutes())
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
