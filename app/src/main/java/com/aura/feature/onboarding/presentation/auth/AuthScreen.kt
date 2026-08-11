package com.aura.feature.onboarding.presentation.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.R
import com.aura.core.designsystem.component.AuraPrimaryButton
import com.aura.core.designsystem.component.AuraSurfaceButton
import com.aura.core.designsystem.component.AuraToastHost
import com.aura.core.designsystem.component.AuraToastKind
import com.aura.core.designsystem.component.AuraToastState
import com.aura.core.designsystem.component.auraGlow
import com.aura.core.designsystem.component.rememberAuraToastState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.onboarding.domain.model.AuthMode
import com.aura.feature.onboarding.presentation.components.AuthSegmentedControl
import com.aura.feature.onboarding.presentation.components.AuthTextField
import com.aura.feature.onboarding.presentation.components.BrandLogoRow
import com.aura.feature.onboarding.presentation.components.BrandTagline
import com.aura.feature.onboarding.presentation.components.OrDivider
import com.aura.feature.onboarding.presentation.components.designBottomGap
import com.aura.feature.onboarding.presentation.components.PASSWORD_PLACEHOLDER

@Composable
fun AuthRoute(
    onOpenHome: () -> Unit,
    onOpenInvite: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastState = rememberAuraToastState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.OpenHome -> onOpenHome()
                AuthEvent.OpenInvite -> onOpenInvite()
                is AuthEvent.ShowToast -> toastState.show(
                    text = context.getString(event.toast.textRes()),
                    kind = event.toast.kind(),
                )
            }
        }
    }

    AuthScreen(
        uiState = uiState,
        actions = AuthActions(
            onModeChange = viewModel::onModeChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = viewModel::onSubmit,
            onGoogleClick = viewModel::onGoogleClick,
            onForgotPasswordClick = viewModel::onForgotPasswordClick,
        ),
        toastState = toastState,
        modifier = modifier,
    )
}

data class AuthActions(
    val onModeChange: (AuthMode) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onGoogleClick: () -> Unit = {},
    val onForgotPasswordClick: () -> Unit = {},
)

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    actions: AuthActions,
    modifier: Modifier = Modifier,
    toastState: AuraToastState = rememberAuraToastState(),
) {
    val colors = AuraTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.authBackground)
                .statusBarsPadding()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 15.5.dp),
            ) {
                Spacer(Modifier.height(53.dp))

                BrandLogoRow()

                Spacer(Modifier.height(10.dp))

                BrandTagline()

                Spacer(Modifier.height(44.dp))

                AuthSegmentedControl(
                    mode = uiState.mode,
                    onModeChange = actions.onModeChange,
                )

                Spacer(Modifier.height(24.dp))

                AuraSurfaceButton(
                    text = stringResource(R.string.auth_google),
                    onClick = actions.onGoogleClick,
                    enabled = !uiState.submitting,
                    leading = {
                        Image(
                            painter = painterResource(R.drawable.ic_google),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                )

                Spacer(Modifier.height(22.dp))

                OrDivider()

                Spacer(Modifier.height(22.dp))

                AuthTextField(
                    label = stringResource(R.string.auth_email),
                    value = uiState.email,
                    onValueChange = actions.onEmailChange,
                    placeholder = stringResource(R.string.auth_email_placeholder),
                    isError = uiState.invalidField == AuthField.EMAIL,
                    imeAction = ImeAction.Next,
                )

                Spacer(Modifier.height(12.dp))

                AuthTextField(
                    label = stringResource(R.string.auth_password),
                    value = uiState.password,
                    onValueChange = actions.onPasswordChange,
                    placeholder = PASSWORD_PLACEHOLDER,
                    isPassword = true,
                    isError = uiState.invalidField == AuthField.PASSWORD,
                    imeAction = ImeAction.Go,
                    onImeAction = actions.onSubmit,
                )

                ForgotPasswordRow(
                    visible = uiState.mode == AuthMode.SIGN_IN,
                    onClick = actions.onForgotPasswordClick,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 17.5.dp)
                    .padding(
                        bottom = designBottomGap(
                            if (uiState.mode == AuthMode.SIGN_UP) 28.dp else 49.dp
                        )
                    )
                    .auraGlow(
                        color = colors.authGlow,
                        width = 260.dp,
                        height = 72.dp,
                        offsetY = if (uiState.mode == AuthMode.SIGN_UP) (-10.5).dp else 0.dp,
                    ),
            ) {
                AuraPrimaryButton(
                    text = stringResource(
                        when (uiState.mode) {
                            AuthMode.SIGN_IN -> R.string.auth_sign_in
                            AuthMode.SIGN_UP -> R.string.auth_create
                        }
                    ),
                    onClick = actions.onSubmit,
                    enabled = !uiState.submitting,
                )

                if (uiState.mode == AuthMode.SIGN_UP) {
                    Spacer(Modifier.height(8.dp))
                    LegalNotice()
                }
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

@Composable
private fun ForgotPasswordRow(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(39.dp),
    ) {
        if (visible) {
            Text(
                text = stringResource(R.string.auth_forgot),
                style = AuraTheme.typography.linkLabel,
                color = colors.authTextMuted,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    ),
            )
        }
    }
}

@Composable
private fun LegalNotice(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors
    val line = stringResource(R.string.auth_terms_line)
    val links = listOf(
        stringResource(R.string.auth_terms_link),
        stringResource(R.string.auth_privacy_link),
    )

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = colors.textPrimary.copy(alpha = 0.42f))) {
                append(line)
            }
            val linkStyle = SpanStyle(color = colors.textPrimary.copy(alpha = 0.62f))
            links.forEach { link ->
                val start = line.indexOf(link)
                if (start >= 0) addStyle(linkStyle, start, start + link.length)
            }
        },
        style = AuraTheme.typography.legalLabel,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@StringRes
private fun AuthToast.textRes(): Int = when (this) {
    AuthToast.EMAIL_REQUIRED -> R.string.toast_email_first
    AuthToast.EMAIL_INVALID -> R.string.toast_email_invalid
    AuthToast.PASSWORD_TOO_SHORT -> R.string.toast_pass_short
    AuthToast.WRONG_CREDENTIALS -> R.string.toast_wrong_creds
    AuthToast.ACCOUNT_EXISTS -> R.string.toast_acc_exists
    AuthToast.NO_ACCOUNT -> R.string.toast_no_account
    AuthToast.NO_CONNECTION -> R.string.toast_no_connection
    AuthToast.RESET_LINK_SENT -> R.string.toast_reset_sent
}

private fun AuthToast.kind(): AuraToastKind = when (this) {
    AuthToast.RESET_LINK_SENT -> AuraToastKind.SUCCESS
    else -> AuraToastKind.ERROR
}

@Preview(widthDp = 375, heightDp = 813)
@Composable
private fun AuthScreenSignInPreview() {
    AuraTheme {
        AuthScreen(uiState = AuthUiState(mode = AuthMode.SIGN_IN), actions = AuthActions())
    }
}

@Preview(widthDp = 375, heightDp = 813)
@Composable
private fun AuthScreenSignUpPreview() {
    AuraTheme {
        AuthScreen(uiState = AuthUiState(mode = AuthMode.SIGN_UP), actions = AuthActions())
    }
}

@Preview(widthDp = 375, heightDp = 813)
@Composable
private fun AuthScreenInvalidEmailPreview() {
    AuraTheme {
        AuthScreen(
            uiState = AuthUiState(
                mode = AuthMode.SIGN_IN,
                email = "said",
                password = "12345678",
                invalidField = AuthField.EMAIL,
            ),
            actions = AuthActions(),
        )
    }
}
