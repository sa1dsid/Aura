package com.aura.feature.onboarding.presentation.auth

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
import androidx.compose.ui.res.painterResource
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
import com.aura.core.designsystem.component.auraGlow
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.onboarding.domain.model.AuthFailure
import com.aura.feature.onboarding.domain.model.AuthMode
import com.aura.feature.onboarding.domain.model.MIN_PASSWORD_LENGTH
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

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.OpenHome -> onOpenHome()
                AuthEvent.OpenInvite -> onOpenInvite()
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
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
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
                text = "Continue with Google",
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
                label = "EMAIL",
                value = uiState.email,
                onValueChange = actions.onEmailChange,
                placeholder = "you@example.com",
                imeAction = ImeAction.Next,
            )

            Spacer(Modifier.height(12.dp))

            AuthTextField(
                label = "PASSWORD",
                value = uiState.password,
                onValueChange = actions.onPasswordChange,
                placeholder = PASSWORD_PLACEHOLDER,
                isPassword = true,
                imeAction = ImeAction.Go,
                onImeAction = actions.onSubmit,
            )

            MessageRow(
                uiState = uiState,
                onForgotPasswordClick = actions.onForgotPasswordClick,
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
                text = when (uiState.mode) {
                    AuthMode.SIGN_IN -> "Sign In"
                    AuthMode.SIGN_UP -> "Create Account"
                },
                onClick = actions.onSubmit,
                enabled = !uiState.submitting,
            )

            if (uiState.mode == AuthMode.SIGN_UP) {
                Spacer(Modifier.height(8.dp))
                LegalNotice()
            }
        }
    }
}

@Composable
private fun MessageRow(
    uiState: AuthUiState,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(39.dp),
    ) {
        uiState.message?.let { message ->
            Text(
                text = message.asText(),
                style = AuraTheme.typography.linkLabel,
                color = if (message is AuthMessage.Failure) colors.danger else colors.authTextMuted,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        if (uiState.mode == AuthMode.SIGN_IN && uiState.message == null) {
            Text(
                text = "Forgot password?",
                style = AuraTheme.typography.linkLabel,
                color = colors.authTextMuted,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onForgotPasswordClick,
                    ),
            )
        }
    }
}

@Composable
private fun LegalNotice(modifier: Modifier = Modifier) {
    val colors = AuraTheme.colors

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = colors.textPrimary.copy(alpha = 0.42f))) {
                append("By signing up you agree to the ")
            }
            withStyle(SpanStyle(color = colors.textPrimary.copy(alpha = 0.62f))) {
                append("Terms of Service")
            }
            withStyle(SpanStyle(color = colors.textPrimary.copy(alpha = 0.42f))) {
                append(" and ")
            }
            withStyle(SpanStyle(color = colors.textPrimary.copy(alpha = 0.62f))) {
                append("Privacy Policy")
            }
        },
        style = AuraTheme.typography.legalLabel,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

private fun AuthMessage.asText(): String = when (this) {
    is AuthMessage.ResetLinkSent -> "Reset link sent to $email"
    is AuthMessage.Failure -> when (failure) {
        AuthFailure.EMAIL_INVALID -> "Enter a valid email"
        AuthFailure.PASSWORD_TOO_SHORT -> "At least $MIN_PASSWORD_LENGTH characters"
        AuthFailure.EMAIL_ALREADY_REGISTERED -> "This email is already registered"
        AuthFailure.ACCOUNT_NOT_FOUND -> "No account for this email"
        AuthFailure.WRONG_PASSWORD -> "Wrong password"
        AuthFailure.GOOGLE_CANCELLED -> "Google sign-in cancelled"
        AuthFailure.NETWORK -> "Network is unavailable"
    }
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
