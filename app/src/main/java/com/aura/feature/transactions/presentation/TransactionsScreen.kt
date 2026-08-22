package com.aura.feature.transactions.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.R
import com.aura.core.designsystem.component.AuraEmptyState
import com.aura.core.designsystem.component.AuraNestedTopBar
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.presentation.HomeTab
import com.aura.feature.home.presentation.components.AuraBottomBar
import com.aura.feature.transactions.domain.model.filterBy
import com.aura.feature.transactions.presentation.components.TransactionFilterRow
import com.aura.feature.transactions.presentation.components.TransactionsLogCard
import com.aura.feature.transactions.presentation.preview.TransactionsPreviewData

private val ScreenPadding = 15.dp

private val TopBarGap = 12.dp

@Composable
fun TransactionsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNewsClick: () -> Unit = {},
    onTabSelected: (HomeTab) -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    TransactionsScreen(
        uiState = uiState,
        actions = TransactionsActions(
            onBackClick = onBack,
            onNewsClick = onNewsClick,
            onFilterClick = viewModel::onFilterClick,
        ),
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    actions: TransactionsActions,
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
        TransactionsContent(
            uiState = uiState,
            actions = actions,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun TransactionsContent(
    uiState: TransactionsUiState,
    actions: TransactionsActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val visibleEvents = remember(uiState.events, uiState.filter) {
        uiState.events.filterBy(uiState.filter)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        AuraNestedTopBar(
            handle = uiState.handle,
            hasUnreadNews = uiState.hasUnreadNews,
            onBackClick = actions.onBackClick,
            onNewsClick = actions.onNewsClick,
        )

        Spacer(Modifier.height(TopBarGap))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = ScreenPadding),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.tx_title),
                    style = AuraTheme.typography.screenHeading,
                    color = colors.textBright,
                )
                Text(
                    text = stringResource(R.string.tx_subtitle),
                    style = AuraTheme.typography.body,
                    color = colors.textSecondary,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.tx_history),
                style = AuraTheme.typography.cardLabel,
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(12.dp))

            if (uiState.events.isEmpty()) {
                AuraEmptyState(
                    iconRes = R.drawable.ic_transaction_minus,
                    title = stringResource(R.string.tx_empty_title),
                    text = stringResource(R.string.tx_empty_text),
                )
            } else {
                TransactionFilterRow(
                    selected = uiState.filter,
                    onFilterClick = actions.onFilterClick,
                )

                Spacer(Modifier.height(12.dp))

                TransactionsLogCard(
                    events = visibleEvents,
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Preview(widthDp = 375, heightDp = 812, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun TransactionsScreenPreview() {
    AuraTheme {
        TransactionsScreen(
            uiState = TransactionsPreviewData.content,
            actions = TransactionsActions(),
        )
    }
}

@Preview(widthDp = 375, heightDp = 812, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun TransactionsScreenEmptyPreview() {
    AuraTheme {
        TransactionsScreen(
            uiState = TransactionsPreviewData.empty,
            actions = TransactionsActions(),
        )
    }
}
