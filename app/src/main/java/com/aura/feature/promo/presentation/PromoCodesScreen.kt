package com.aura.feature.promo.presentation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.R
import com.aura.core.designsystem.component.AuraEmptyState
import com.aura.core.designsystem.component.AuraNestedTopBar
import com.aura.core.designsystem.component.AuraToastHost
import com.aura.core.designsystem.component.AuraToastKind
import com.aura.core.designsystem.component.AuraToastState
import com.aura.core.designsystem.component.rememberAuraToastState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.home.presentation.HomeTab
import com.aura.feature.home.presentation.components.AuraBottomBar
import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.promo.domain.model.PromoCodeKind
import com.aura.feature.promo.domain.model.ofKind
import com.aura.feature.promo.presentation.components.PromoTicket
import com.aura.feature.promo.presentation.preview.PromoPreviewData

private val ScreenPadding = 15.dp

private const val VISIBLE_TICKETS = 3

@Composable
fun PromoCodesRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onTabSelected: (HomeTab) -> Unit = {},
    viewModel: PromoCodesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastState = rememberAuraToastState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    PromoCodesScreen(
        uiState = uiState,
        actions = PromoCodesActions(
            onBackClick = onBack,
            onCodeClick = { code ->
                clipboard.setText(AnnotatedString(code.code))
                toastState.show(
                    text = context.getString(R.string.promo_copied),
                    kind = AuraToastKind.SUCCESS,
                )
            },
        ),
        onTabSelected = onTabSelected,
        toastState = toastState,
        modifier = modifier,
    )
}

@Composable
fun PromoCodesScreen(
    uiState: PromoCodesUiState,
    actions: PromoCodesActions,
    modifier: Modifier = Modifier,
    onTabSelected: (HomeTab) -> Unit = {},
    toastState: AuraToastState = rememberAuraToastState(),
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
        Box(Modifier.fillMaxSize()) {
            PromoCodesContent(
                uiState = uiState,
                actions = actions,
                contentPadding = innerPadding,
            )

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
private fun PromoCodesContent(
    uiState: PromoCodesUiState,
    actions: PromoCodesActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding),
    ) {
        AuraNestedTopBar(
            handle = uiState.handle,
            hasUnreadNews = uiState.hasUnreadNews,
            onBackClick = actions.onBackClick,
            onNewsClick = actions.onNewsClick,
        )

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.promo_title),
                style = AuraTheme.typography.screenHeading,
                color = colors.textBright,
            )
            Text(
                text = stringResource(R.string.promo_subtitle),
                style = AuraTheme.typography.body,
                color = colors.textSecondary,
            )
        }

        Spacer(Modifier.height(16.dp))

        if (uiState.codes.isEmpty()) {
            AuraEmptyState(
                iconRes = R.drawable.ic_ticket_expired,
                title = stringResource(R.string.promo_empty_title),
                text = stringResource(R.string.promo_empty_text),
            )
        } else {
            PromoSection(
                labelRes = R.string.promo_section_spark,
                moreRes = R.string.promo_more_spark,
                codes = uiState.codes.ofKind(PromoCodeKind.SPARK),
                onCodeClick = actions.onCodeClick,
            )

            Spacer(Modifier.height(16.dp))

            PromoSection(
                labelRes = R.string.promo_section_vpn,
                moreRes = R.string.promo_more_vpn,
                codes = uiState.codes.ofKind(PromoCodeKind.VPN),
                onCodeClick = actions.onCodeClick,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PromoSection(
    @StringRes labelRes: Int,
    @StringRes moreRes: Int,
    codes: List<PromoCode>,
    onCodeClick: (PromoCode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (codes.isEmpty()) return

    val colors = AuraTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(labelRes),
            style = AuraTheme.typography.cardLabel,
            color = colors.textSecondary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            codes.forEach { code ->
                PromoTicket(code = code, onClick = { onCodeClick(code) })
            }
        }

        if (codes.size > VISIBLE_TICKETS) {
            Text(
                text = stringResource(moreRes, codes.size - VISIBLE_TICKETS),
                style = AuraTheme.typography.caption,
                color = colors.textDisabled,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(widthDp = 375, heightDp = 812, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun PromoCodesScreenPreview() {
    AuraTheme {
        PromoCodesScreen(
            uiState = PromoPreviewData.content,
            actions = PromoCodesActions(),
        )
    }
}

@Preview(widthDp = 375, heightDp = 812, showBackground = true, backgroundColor = 0xFF030507)
@Composable
private fun PromoCodesScreenEmptyPreview() {
    AuraTheme {
        PromoCodesScreen(
            uiState = PromoPreviewData.empty,
            actions = PromoCodesActions(),
        )
    }
}
