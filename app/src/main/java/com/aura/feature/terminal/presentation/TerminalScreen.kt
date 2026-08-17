package com.aura.feature.terminal.presentation

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.R
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.presentation.HomeTab
import com.aura.feature.home.presentation.components.AuraBottomBar
import com.aura.feature.terminal.domain.model.TerminalCounters
import com.aura.feature.terminal.presentation.components.TerminalEntryCards
import com.aura.feature.terminal.presentation.components.TerminalTopBar

private val ScreenPadding = 15.dp

@Composable
fun TerminalRoute(
    onOpenTransactions: () -> Unit,
    onOpenPromoCodes: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onTabSelected: (HomeTab) -> Unit = {},
    viewModel: TerminalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TerminalScreen(
        uiState = uiState,
        actions = TerminalActions(
            onMenuClick = onMenuClick,
            onTransactionsClick = {
                viewModel.onTransactionsOpened()
                onOpenTransactions()
            },
            onPromoCodesClick = {
                viewModel.onPromoCodesOpened()
                onOpenPromoCodes()
            },
        ),
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

@Composable
fun TerminalScreen(
    uiState: TerminalUiState,
    actions: TerminalActions,
    modifier: Modifier = Modifier,
    onTabSelected: (HomeTab) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = AuraTheme.colors.background,
        bottomBar = {
            AuraBottomBar(
                selected = HomeTab.TERMINAL,
                onTabSelected = onTabSelected,
            )
        },
    ) { innerPadding ->
        TerminalContent(
            uiState = uiState,
            actions = actions,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun TerminalContent(
    uiState: TerminalUiState,
    actions: TerminalActions,
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
        TerminalTopBar(
            hasUnreadNews = uiState.hasUnreadNews,
            onMenuClick = actions.onMenuClick,
            onNewsClick = actions.onNewsClick,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.terminal_title),
            style = AuraTheme.typography.screenHeading,
            color = AuraTheme.colors.textBright,
        )

        Spacer(Modifier.height(12.dp))

        TerminalEntryCards(
            counters = uiState.counters,
            onTransactionsClick = actions.onTransactionsClick,
            onPromoCodesClick = actions.onPromoCodesClick,
        )
    }
}

@Preview(widthDp = 375, heightDp = 812, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun TerminalScreenPreview() {
    AuraTheme {
        TerminalScreen(
            uiState = TerminalUiState(
                counters = TerminalCounters(unreadTransactions = 105, unreadPromoCodes = 2),
            ),
            actions = TerminalActions(),
        )
    }
}

@Preview(widthDp = 375, heightDp = 812, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun TerminalScreenEmptyPreview() {
    AuraTheme {
        TerminalScreen(
            uiState = TerminalUiState(counters = TerminalCounters()),
            actions = TerminalActions(),
        )
    }
}
