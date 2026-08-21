package com.aura.feature.account.presentation.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.BuildConfig
import com.aura.R
import com.aura.core.designsystem.component.AuraBottomSheet
import com.aura.core.designsystem.component.AuraSwitch
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.core.system.openUrl
import com.aura.feature.account.presentation.menu.components.AccountMenuHeader
import com.aura.feature.account.presentation.menu.components.AccountMenuRow
import com.aura.feature.account.presentation.preview.AccountMenuPreviewData

@Composable
fun AccountMenuRoute(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onSessionClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountMenuViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AccountMenuEvent.OpenUrl -> context.openUrl(event.url)
                AccountMenuEvent.SessionClosed -> onSessionClosed()
            }
        }
    }

    LaunchedEffect(visible) {
        if (visible) viewModel.onMenuOpened() else viewModel.onMenuDismissed()
    }

    AccountMenuSheet(
        visible = visible,
        state = uiState,
        actions = AccountMenuActions(
            onPushNotificationsChange = viewModel::onPushNotificationsChange,
            onTermsClick = viewModel::onTermsClick,
            onPrivacyClick = viewModel::onPrivacyClick,
            onLogOutClick = viewModel::onLogOutClick,
            onDeleteAccountClick = viewModel::onDeleteAccountClick,
            onKeepAccountClick = viewModel::onKeepAccountClick,
            onDeletePermanentlyClick = viewModel::onDeletePermanentlyClick,
            onDismissRequest = onDismissRequest,
        ),
        modifier = modifier,
    )
}

@Composable
fun AccountMenuSheet(
    visible: Boolean,
    state: AccountMenuUiState,
    actions: AccountMenuActions,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    AuraBottomSheet(
        visible = visible && !state.isDeleteConfirmVisible,
        onDismissRequest = actions.onDismissRequest,
        modifier = modifier,
    ) {
        AccountMenuHeader(profile = state.profile)

        Spacer(Modifier.height(16.dp))

        Column(Modifier.fillMaxWidth()) {
            AccountMenuRow(
                iconRes = R.drawable.ic_notification,
                label = stringResource(R.string.menu_push),
                onClick = { actions.onPushNotificationsChange(!state.pushNotifications) },
                role = Role.Switch,
                trailingGap = 16.dp,
                trailing = { AuraSwitch(checked = state.pushNotifications) },
            )

            AccountMenuRow(
                iconRes = R.drawable.ic_clipboard_text,
                label = stringResource(R.string.menu_terms),
                onClick = actions.onTermsClick,
                trailing = { RowChevron() },
            )

            AccountMenuRow(
                iconRes = R.drawable.ic_shield,
                label = stringResource(R.string.menu_privacy),
                onClick = actions.onPrivacyClick,
                trailing = { RowChevron() },
            )

            AccountMenuRow(
                iconRes = R.drawable.ic_logout,
                label = stringResource(R.string.menu_logout),
                onClick = actions.onLogOutClick,
            )

            AccountMenuRow(
                iconRes = R.drawable.ic_trash,
                label = stringResource(R.string.menu_delete),
                onClick = actions.onDeleteAccountClick,
                labelColor = colors.warning,
                iconColor = colors.warning,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.menu_version, BuildConfig.VERSION_NAME),
            style = AuraTheme.typography.body,
            color = colors.sheetMuted,
            textAlign = TextAlign.Center,
        )
    }

    DeleteAccountDialog(
        visible = state.isDeleteConfirmVisible,
        onKeepClick = actions.onKeepAccountClick,
        onDeleteClick = actions.onDeletePermanentlyClick,
    )
}

@Composable
private fun RowChevron() {
    Icon(
        painter = painterResource(R.drawable.ic_arrow_right),
        contentDescription = null,
        tint = AuraTheme.colors.textSecondary,
        modifier = Modifier.size(24.dp),
    )
}

@Preview(widthDp = 375, heightDp = 815)
@Composable
private fun AccountMenuSheetPreview() {
    AuraTheme {
        AccountMenuSheet(
            visible = true,
            state = AccountMenuPreviewData.state,
            actions = AccountMenuActions(),
        )
    }
}

@Preview(widthDp = 375, heightDp = 815)
@Composable
private fun AccountMenuSheetPushOffPreview() {
    AuraTheme {
        AccountMenuSheet(
            visible = true,
            state = AccountMenuPreviewData.state.copy(pushNotifications = false),
            actions = AccountMenuActions(),
        )
    }
}
