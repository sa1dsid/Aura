package com.aura.feature.onboarding.presentation.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.R
import com.aura.core.designsystem.component.AuraOutlinedButton
import com.aura.core.designsystem.component.AuraPrimaryButton
import com.aura.core.designsystem.component.auraGlow
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.onboarding.domain.model.INVITE_CODE_LENGTH
import com.aura.feature.onboarding.domain.model.InviteFailure
import com.aura.feature.onboarding.presentation.components.InviteCodeField
import com.aura.feature.onboarding.presentation.components.designBottomGap

@Composable
fun InviteRoute(
    onFinished: (bonusPopupPending: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is InviteEvent.Finished -> onFinished(event.bonusPopupPending)
            }
        }
    }

    InviteScreen(
        uiState = uiState,
        actions = InviteActions(
            onCodeChange = viewModel::onCodeChange,
            onPaste = { viewModel.onPaste(clipboard.getText()?.text) },
            onApplyClick = viewModel::onApplyClick,
            onSkipClick = viewModel::onSkipClick,
        ),
        modifier = modifier,
    )
}

data class InviteActions(
    val onCodeChange: (String) -> Unit = {},
    val onPaste: () -> Unit = {},
    val onApplyClick: () -> Unit = {},
    val onSkipClick: () -> Unit = {},
)

@Composable
fun InviteScreen(
    uiState: InviteUiState,
    actions: InviteActions,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.authBackground)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Spacer(Modifier.height(185.5.dp))

        Column(modifier = Modifier.padding(horizontal = 25.7.dp)) {
            Text(
                text = stringResource(R.string.invite_title),
                style = AuraTheme.typography.screenHeading,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.invite_sub),
                style = AuraTheme.typography.screenSubheading,
                color = colors.authTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(33.5.dp))

        InviteCodeField(
            code = uiState.code,
            onCodeChange = actions.onCodeChange,
            onPaste = actions.onPaste,
            locked = uiState.locked,
            modifier = Modifier.padding(horizontal = 33.5.dp),
        )

        Spacer(Modifier.height(12.5.dp))

        Text(
            text = uiState.failure?.let { stringResource(it.textRes()) }
                ?: stringResource(R.string.invite_once_note),
            style = AuraTheme.typography.screenHint,
            color = if (uiState.failure == null) colors.authTextDim else colors.danger,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.7.dp),
        )

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
                .padding(bottom = designBottomGap(29.dp))
                .auraGlow(
                    color = colors.authGlow,
                    width = 260.dp,
                    height = 72.dp,
                    offsetY = 32.5.dp,
                ),
        ) {
            AuraPrimaryButton(
                text = stringResource(R.string.invite_apply),
                onClick = actions.onApplyClick,
                enabled = uiState.code.length >= INVITE_CODE_LENGTH && !uiState.submitting,
            )

            Spacer(Modifier.height(12.dp))

            AuraOutlinedButton(
                text = stringResource(R.string.invite_skip),
                onClick = actions.onSkipClick,
                enabled = !uiState.submitting,
            )
        }
    }
}

private fun InviteFailure.textRes(): Int = when (this) {
    InviteFailure.UNKNOWN_CODE,
    InviteFailure.OWN_CODE,
    InviteFailure.OWNER_DELETED,
    InviteFailure.ALREADY_APPLIED,
    -> R.string.toast_code_invalid

    InviteFailure.NETWORK -> R.string.toast_no_connection
}

@Preview(widthDp = 375, heightDp = 820)
@Composable
private fun InviteScreenEmptyPreview() {
    AuraTheme {
        InviteScreen(uiState = InviteUiState(), actions = InviteActions())
    }
}

@Preview(widthDp = 375, heightDp = 820)
@Composable
private fun InviteScreenFilledPreview() {
    AuraTheme {
        InviteScreen(uiState = InviteUiState(code = "SYREX482"), actions = InviteActions())
    }
}

@Preview(widthDp = 375, heightDp = 820)
@Composable
private fun InviteScreenCodeAppliedPreview() {
    AuraTheme {
        InviteScreen(
            uiState = InviteUiState(code = "SYREX482", locked = true),
            actions = InviteActions(),
        )
    }
}
