package com.aura.feature.network.presentation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.aura.core.system.openVpnSettings
import com.aura.core.system.shareText
import com.aura.feature.home.presentation.HomeTab
import com.aura.feature.home.presentation.components.AuraBottomBar
import com.aura.feature.network.domain.model.SpeedTestFailure
import com.aura.feature.network.domain.model.SpeedTestResult
import com.aura.feature.network.presentation.components.DataCardsGrid
import com.aura.feature.network.presentation.components.DiagnosticsCard
import com.aura.feature.network.presentation.components.MetricCardsRow
import com.aura.feature.network.presentation.components.NetworkHeader
import com.aura.feature.network.presentation.components.NetworkLogCard
import com.aura.feature.network.presentation.components.NetworkTopBar
import com.aura.feature.network.presentation.components.PingHistoryCard
import com.aura.feature.network.presentation.format.formatSpeed
import com.aura.feature.network.presentation.preview.NetworkPreviewData

private val ScreenPadding = 15.dp

@Composable
fun NetworkRoute(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onTabSelected: (HomeTab) -> Unit = {},
    viewModel: NetworkViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastState = rememberAuraToastState()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenResumed()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NetworkEvent.DiagnosticsFailed -> toastState.show(
                    text = context.failureText(event.failure),
                    kind = AuraToastKind.ERROR,
                )

                is NetworkEvent.ShareResult -> context.shareText(event.result.shareText(context))

                NetworkEvent.OpenVpnSettings -> context.openVpnSettings()
            }
        }
    }

    NetworkScreen(
        uiState = uiState,
        actions = NetworkActions(
            onMenuClick = onMenuClick,
            onVpnCardClick = viewModel::onVpnCardClick,
            onStartTestClick = viewModel::onStartTestClick,
            onShareResultClick = viewModel::onShareResultClick,
        ),
        onTabSelected = onTabSelected,
        toastState = toastState,
        modifier = modifier,
    )
}

@Composable
fun NetworkScreen(
    uiState: NetworkUiState,
    actions: NetworkActions,
    modifier: Modifier = Modifier,
    onTabSelected: (HomeTab) -> Unit = {},
    toastState: AuraToastState = rememberAuraToastState(),
) {
    val colors = AuraTheme.colors

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        bottomBar = {
            AuraBottomBar(
                selected = HomeTab.NETWORK,
                onTabSelected = onTabSelected,
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            when (uiState) {
                NetworkUiState.Loading -> LoadingState(Modifier.padding(innerPadding))
                is NetworkUiState.Content -> NetworkContent(
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
                    .padding(horizontal = ScreenPadding, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun NetworkContent(
    state: NetworkUiState.Content,
    actions: NetworkActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding),
    ) {
        NetworkTopBar(
            handle = state.handle,
            hasUnreadNews = state.hasUnreadNews,
            onMenuClick = actions.onMenuClick,
            onNewsClick = actions.onNewsClick,
        )

        Spacer(Modifier.height(20.dp))

        NetworkHeader(lastTestedAt = state.lastTestedAt)

        Spacer(Modifier.height(12.dp))

        MetricCardsRow(metrics = state.metrics)

        Spacer(Modifier.height(16.dp))

        DataCardsGrid(
            connection = state.connection,
            onVpnCardClick = actions.onVpnCardClick,
        )

        Spacer(Modifier.height(16.dp))

        SectionLabel(text = stringResource(R.string.net_history))

        Spacer(Modifier.height(12.dp))

        PingHistoryCard(history = state.history)

        Spacer(Modifier.height(16.dp))

        NetworkLogCard(history = state.history)

        Spacer(Modifier.height(16.dp))

        SectionLabel(text = stringResource(R.string.diag_section))

        Spacer(Modifier.height(10.dp))

        DiagnosticsCard(
            diagnostics = state.diagnostics,
            onStartTestClick = actions.onStartTestClick,
            onShareResultClick = actions.onShareResultClick,
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AuraTheme.typography.cardLabel,
        color = AuraTheme.colors.textSecondary,
        modifier = modifier,
    )
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

private fun Context.failureText(failure: SpeedTestFailure): String = when (failure) {
    SpeedTestFailure.NO_CONNECTION -> getString(R.string.toast_no_connection)
    SpeedTestFailure.INTERRUPTED -> getString(R.string.toast_test_failed)
}

private fun SpeedTestResult.shareText(context: Context): String = context.getString(
    R.string.diag_share_text,
    downloadMbps.formatSpeed(),
    uploadMbps.formatSpeed(),
    pingMs.toString(),
)

@Preview(widthDp = 375, heightDp = 1810, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun NetworkScreenPreview() {
    AuraTheme {
        NetworkScreen(
            uiState = NetworkPreviewData.content,
            actions = NetworkActions(),
        )
    }
}

@Preview(widthDp = 375, heightDp = 1900, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun NetworkScreenResultPreview() {
    AuraTheme {
        NetworkScreen(
            uiState = NetworkPreviewData.vpnOnWithResult,
            actions = NetworkActions(),
        )
    }
}
