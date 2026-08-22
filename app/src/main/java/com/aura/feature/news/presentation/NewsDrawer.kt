package com.aura.feature.news.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.R
import com.aura.core.designsystem.component.AuraTopDrawer
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.news.presentation.components.NewsCard
import com.aura.feature.news.presentation.preview.NewsPreviewData

private val DrawerPadding = 16.dp

private val SectionGap = 10.dp

private val CardGap = 8.dp

private val BottomPadding = 12.dp

private const val LIST_MAX_HEIGHT_FRACTION = 0.65f

@Composable
fun NewsDrawerRoute(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(visible) {
        if (visible) viewModel.onDrawerOpened() else viewModel.onDrawerDismissed()
    }

    NewsDrawer(
        visible = visible,
        state = uiState,
        actions = NewsActions(
            onCardClick = viewModel::onCardClick,
            onDismissRequest = onDismissRequest,
        ),
        modifier = modifier,
    )
}

@Composable
fun NewsDrawer(
    visible: Boolean,
    state: NewsUiState,
    actions: NewsActions,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val listMaxHeight = LocalConfiguration.current.screenHeightDp.dp * LIST_MAX_HEIGHT_FRACTION

    AuraTopDrawer(
        visible = visible,
        onDismissRequest = actions.onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DrawerPadding)
                .padding(bottom = BottomPadding)
        ) {
            Text(
                text = stringResource(R.string.news_title),
                style = AuraTheme.typography.cardLabel,
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(SectionGap))

            if (state.items.isEmpty()) {
                Text(
                    text = stringResource(R.string.news_empty),
                    style = AuraTheme.typography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = listMaxHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CardGap),
                ) {
                    state.items.forEach { item ->
                        NewsCard(
                            item = item,
                            expanded = item.id == state.expandedId,
                            onClick = { actions.onCardClick(item) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(SectionGap))

            Text(
                text = stringResource(R.string.news_hint),
                style = AuraTheme.typography.cardCaption,
                color = colors.textDisabled,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(widthDp = 375, heightDp = 813)
@Composable
private fun NewsDrawerPreview() {
    AuraTheme {
        NewsDrawer(
            visible = true,
            state = NewsPreviewData.state,
            actions = NewsActions(),
        )
    }
}

@Preview(widthDp = 375, heightDp = 813)
@Composable
private fun NewsDrawerExpandedPreview() {
    AuraTheme {
        NewsDrawer(
            visible = true,
            state = NewsPreviewData.state.copy(expandedId = "1"),
            actions = NewsActions(),
        )
    }
}
