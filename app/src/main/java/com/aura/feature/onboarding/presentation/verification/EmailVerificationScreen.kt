package com.aura.feature.onboarding.presentation.verification

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.R
import com.aura.core.designsystem.component.AuraOutlinedButton
import com.aura.core.designsystem.component.AuraPrimaryButton
import com.aura.core.designsystem.component.AuraToastHost
import com.aura.core.designsystem.component.AuraToastKind
import com.aura.core.designsystem.component.AuraToastState
import com.aura.core.designsystem.component.auraGlow
import com.aura.core.designsystem.component.rememberAuraToastState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.onboarding.domain.model.EmailVerification
import com.aura.feature.onboarding.domain.model.EmailVerificationFailure
import com.aura.feature.onboarding.domain.model.isWholeEmailCode
import com.aura.feature.onboarding.presentation.components.EmailCodeField
import com.aura.feature.onboarding.presentation.components.designBottomGap

@Composable
fun EmailVerificationRoute(
    verification: EmailVerification,
    onConfirmed: (invitePending: Boolean) -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailVerificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastState = rememberAuraToastState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(verification) {
        viewModel.onScreenOpened(verification)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EmailVerificationEvent.Confirmed -> onConfirmed(event.invitePending)
                EmailVerificationEvent.Cancelled -> onCancelled()
                EmailVerificationEvent.CodeSent -> toastState.show(
                    text = context.getString(R.string.toast_code_sent),
                    kind = AuraToastKind.SUCCESS,
                )
            }
        }
    }

    BackHandler(onBack = viewModel::onCancel)

    EmailVerificationScreen(
        uiState = uiState,
        actions = EmailVerificationActions(
            onCodeChange = viewModel::onCodeChange,
            onPaste = { viewModel.onPaste(clipboard.getText()?.text) },
            onConfirmClick = viewModel::onConfirmClick,
            onResendClick = viewModel::onResendClick,
        ),
        toastState = toastState,
        modifier = modifier,
    )
}

data class EmailVerificationActions(
    val onCodeChange: (String) -> Unit = {},
    val onPaste: () -> Unit = {},
    val onConfirmClick: () -> Unit = {},
    val onResendClick: () -> Unit = {},
)

@Composable
fun EmailVerificationScreen(
    uiState: EmailVerificationUiState,
    actions: EmailVerificationActions,
    modifier: Modifier = Modifier,
    toastState: AuraToastState = rememberAuraToastState(),
) {
    val colors = AuraTheme.colors
    val density = LocalDensity.current
    var actionsHeight by remember { mutableStateOf(0.dp) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.authBackground)
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .consumeWindowInsets(PaddingValues(bottom = actionsHeight))
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(185.5.dp))

                Column(modifier = Modifier.padding(horizontal = 25.7.dp)) {
                    Text(
                        text = stringResource(R.string.verify_title),
                        style = AuraTheme.typography.screenHeading,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = stringResource(
                            R.string.verify_sub,
                            uiState.verification.email,
                        ),
                        style = AuraTheme.typography.screenSubheading,
                        color = colors.authTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(33.5.dp))

                EmailCodeField(
                    code = uiState.code,
                    onCodeChange = actions.onCodeChange,
                    onPaste = actions.onPaste,
                    onSubmit = actions.onConfirmClick,
                    modifier = Modifier.padding(horizontal = 33.5.dp),
                )

                Spacer(Modifier.height(12.5.dp))

                Text(
                    text = uiState.failure?.let { stringResource(it.textRes()) }
                        ?: stringResource(
                            R.string.verify_expiry_note,
                            uiState.verification.codeLifetime.inWholeMinutes.toInt(),
                        ),
                    style = AuraTheme.typography.screenHint,
                    color = if (uiState.failure == null) colors.authTextDim else colors.danger,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 25.7.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { actionsHeight = with(density) { it.height.toDp() } }
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
                    text = stringResource(R.string.verify_confirm),
                    onClick = actions.onConfirmClick,
                    enabled = uiState.code.isWholeEmailCode && !uiState.submitting,
                )

                Spacer(Modifier.height(12.dp))

                AuraOutlinedButton(
                    text = stringResource(R.string.verify_resend),
                    onClick = actions.onResendClick,
                    enabled = !uiState.submitting,
                )
            }
        }

        AuraToastHost(
            state = toastState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 15.5.dp)
                .padding(top = 12.dp),
        )
    }
}

private fun EmailVerificationFailure.textRes(): Int = when (this) {
    EmailVerificationFailure.CODE_REJECTED -> R.string.verify_code_invalid
    EmailVerificationFailure.RESEND_TOO_SOON -> R.string.verify_resend_soon
    EmailVerificationFailure.NETWORK -> R.string.toast_no_connection
}

@Preview(widthDp = 375, heightDp = 820)
@Composable
private fun EmailVerificationEmptyPreview() {
    AuraTheme {
        EmailVerificationScreen(
            uiState = EmailVerificationUiState(
                verification = EmailVerification("said@ioaura.app"),
            ),
            actions = EmailVerificationActions(),
        )
    }
}

@Preview(widthDp = 375, heightDp = 820)
@Composable
private fun EmailVerificationFilledPreview() {
    AuraTheme {
        EmailVerificationScreen(
            uiState = EmailVerificationUiState(
                verification = EmailVerification("said@ioaura.app"),
                code = "482913",
            ),
            actions = EmailVerificationActions(),
        )
    }
}

@Preview(widthDp = 375, heightDp = 820)
@Composable
private fun EmailVerificationRejectedPreview() {
    AuraTheme {
        EmailVerificationScreen(
            uiState = EmailVerificationUiState(
                verification = EmailVerification("said@ioaura.app"),
                code = "482913",
                failure = EmailVerificationFailure.CODE_REJECTED,
            ),
            actions = EmailVerificationActions(),
        )
    }
}
