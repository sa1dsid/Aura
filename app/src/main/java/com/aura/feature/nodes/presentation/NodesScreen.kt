package com.aura.feature.nodes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.R
import com.aura.core.designsystem.component.AuraEmptyState
import com.aura.core.designsystem.component.AuraToastHost
import com.aura.core.designsystem.component.AuraToastKind
import com.aura.core.designsystem.component.AuraToastState
import com.aura.core.designsystem.component.rememberAuraToastState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.core.system.openSocialLink
import com.aura.core.system.shareText
import com.aura.feature.home.presentation.HomeTab
import com.aura.feature.home.presentation.components.AuraBottomBar
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.presentation.components.ActiveSquadCard
import com.aura.feature.nodes.presentation.components.EarnedCard
import com.aura.feature.nodes.presentation.components.FriendRow
import com.aura.feature.nodes.presentation.components.InviteCard
import com.aura.feature.nodes.presentation.components.NodesStatsRow
import com.aura.feature.nodes.presentation.components.NodesTopBar
import com.aura.feature.nodes.presentation.components.SocialSection
import com.aura.feature.nodes.presentation.preview.NodesPreviewData

private val ScreenPadding = 15.dp

private val SectionGap = 16.dp

private val EmptyTextWidth = 250.dp

@Composable
fun NodesRoute(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onTabSelected: (HomeTab) -> Unit = {},
    viewModel: NodesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastState = rememberAuraToastState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenResumed()
    }

    val invite = (uiState as? NodesUiState.Content)?.nodes?.invite

    NodesScreen(
        uiState = uiState,
        actions = NodesActions(
            onMenuClick = onMenuClick,
            onCodeClick = {
                val code = invite?.code ?: return@NodesActions
                clipboard.setText(AnnotatedString(code))
                toastState.show(
                    text = context.getString(R.string.nodes_code_copied),
                    kind = AuraToastKind.SUCCESS,
                )
            },
            onShareClick = {
                val offer = invite ?: return@NodesActions
                context.shareText(
                    offer.shareText ?: context.getString(R.string.nodes_share_text, offer.link),
                )
            },
            onSocialClick = { link -> context.openSocialLink(link.appUrl, link.webUrl) },
        ),
        onTabSelected = onTabSelected,
        toastState = toastState,
        modifier = modifier,
    )
}

@Composable
fun NodesScreen(
    uiState: NodesUiState,
    actions: NodesActions,
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
                selected = HomeTab.NODES,
                onTabSelected = onTabSelected,
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            when (uiState) {
                NodesUiState.Loading -> LoadingState(Modifier.padding(innerPadding))
                is NodesUiState.Content -> NodesContent(
                    nodes = uiState.nodes,
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
private fun NodesContent(
    nodes: NodesState,
    actions: NodesActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        item {
            NodesTopBar(
                handle = nodes.handle,
                hasUnreadNews = nodes.hasUnreadNews,
                onMenuClick = actions.onMenuClick,
                onNewsClick = actions.onNewsClick,
            )

            Spacer(Modifier.height(12.dp))

            Column(Modifier.padding(horizontal = ScreenPadding)) {
                NodesHeader()

                Spacer(Modifier.height(SectionGap))

                InviteCard(
                    invite = nodes.invite,
                    onCodeClick = actions.onCodeClick,
                    onShareClick = actions.onShareClick,
                )
            }
        }

        if (nodes.isEmpty) {
            item {
                Spacer(Modifier.height(SectionGap))

                AuraEmptyState(
                    iconRes = R.drawable.ic_users,
                    title = stringResource(R.string.nodes_empty_title),
                    text = stringResource(R.string.nodes_empty_text),
                    modifier = Modifier.padding(horizontal = ScreenPadding),
                    textWidth = EmptyTextWidth,
                )
            }
        } else {
            item {
                Column(Modifier.padding(horizontal = ScreenPadding)) {
                    Spacer(Modifier.height(SectionGap))

                    NodesStatsRow(state = nodes)

                    Spacer(Modifier.height(SectionGap))

                    EarnedCard(
                        rewards = nodes.rewards,
                        tier = nodes.tier,
                        rates = nodes.tierRates,
                    )
                }
            }

            val squad = nodes.activeSquad
            if (squad.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(SectionGap))

                    ActiveSquadCard(
                        tier = nodes.tier,
                        activeFriends = nodes.activeFriends,
                        squad = squad,
                        modifier = Modifier.padding(horizontal = ScreenPadding),
                    )
                }
            }

            item {
                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.nodes_friends).uppercase(),
                    style = AuraTheme.typography.cardLabel,
                    color = AuraTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = ScreenPadding),
                )

                Spacer(Modifier.height(8.dp))
            }

            itemsIndexed(items = nodes.friends, key = { _, friend -> friend.id }) { index, friend ->
                if (index > 0) Spacer(Modifier.height(8.dp))

                FriendRow(friend = friend)
            }
        }

        item {
            Spacer(Modifier.height(SectionGap))

            SocialSection(
                socials = nodes.socials,
                handle = nodes.handle,
                onSocialClick = actions.onSocialClick,
                modifier = Modifier.padding(horizontal = ScreenPadding),
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun NodesHeader(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.nodes_title),
            style = AuraTheme.typography.screenHeading,
            color = colors.textBright,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.nodes_subtitle),
            style = AuraTheme.typography.body,
            color = colors.textSecondary,
        )
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

@Preview(widthDp = 375, heightDp = 1620, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun NodesScreenPreview() {
    AuraTheme {
        NodesScreen(uiState = NodesPreviewData.content, actions = NodesActions())
    }
}

@Preview(widthDp = 375, heightDp = 1620, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun NodesScreenPrimePreview() {
    AuraTheme {
        NodesScreen(uiState = NodesPreviewData.prime, actions = NodesActions())
    }
}

@Preview(widthDp = 375, heightDp = 1160, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun NodesScreenEmptyPreview() {
    AuraTheme {
        NodesScreen(uiState = NodesPreviewData.empty, actions = NodesActions())
    }
}
