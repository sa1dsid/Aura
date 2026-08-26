package com.aura.feature.ioni.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.aura.core.designsystem.component.AuraToastHost
import com.aura.core.designsystem.component.AuraToastKind
import com.aura.core.designsystem.component.AuraToastState
import com.aura.core.designsystem.component.rememberAuraToastState
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.core.system.isPackageInstalled
import com.aura.core.system.openApp
import com.aura.core.system.openStorePage
import com.aura.feature.home.presentation.HomeTab
import com.aura.feature.home.presentation.components.AuraBottomBar
import com.aura.feature.home.presentation.components.IoniSheet
import com.aura.feature.home.presentation.components.IoniSheetKind
import com.aura.feature.ioni.presentation.components.IoniDialogue
import com.aura.feature.ioni.presentation.components.IoniInputField
import com.aura.feature.ioni.presentation.components.IoniQuestionChips
import com.aura.feature.ioni.presentation.components.IoniSupportPill
import com.aura.feature.ioni.presentation.components.IoniTopBar
import com.aura.feature.ioni.presentation.components.IoniWatermark

@Composable
fun IoniRoute(
    modifier: Modifier = Modifier,
    onTabSelected: (HomeTab) -> Unit = {},
    viewModel: IoniViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastState = rememberAuraToastState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val ioniPackage = stringResource(R.string.ioni_app_package)

    var sheet by remember { mutableStateOf<IoniSheetKind?>(null) }
    var isIoniInstalled by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenResumed()
        if (sheet != null) isIoniInstalled = context.isPackageInstalled(ioniPackage)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onScreenLeft()
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onScreenLeft() }
    }

    IoniScreen(
        uiState = uiState,
        actions = IoniActions(
            onTabSelected = { tab ->
                if (tab != IoniTab.AI) {
                    viewModel.onTabSelected(tab)
                    return@IoniActions
                }
                if (!uiState.isAiReleased) {
                    sheet = IoniSheetKind.COMING
                    return@IoniActions
                }
                isIoniInstalled = context.isPackageInstalled(ioniPackage)
                if (isIoniInstalled) context.openApp(ioniPackage) else sheet = IoniSheetKind.LIVE
            },
            onQuestionClick = viewModel::onQuestionClick,
            onInputChange = viewModel::onInputChange,
            onSendClick = viewModel::onSendClick,
            onSupportClick = viewModel::onSupportClick,
            onEmailClick = { email ->
                clipboard.setText(AnnotatedString(email))
                toastState.show(
                    text = context.getString(R.string.toast_email_copied),
                    kind = AuraToastKind.SUCCESS,
                )
            },
        ),
        onHomeTabSelected = onTabSelected,
        toastState = toastState,
        modifier = modifier,
    )

    IoniSheet(
        kind = sheet,
        isAppInstalled = isIoniInstalled,
        onDismissRequest = { sheet = null },
        onOpenIoniClick = {
            val opened = isIoniInstalled && context.openApp(ioniPackage)
            if (!opened) context.openStorePage(ioniPackage)
        },
    )
}

@Composable
fun IoniScreen(
    uiState: IoniUiState,
    actions: IoniActions,
    modifier: Modifier = Modifier,
    onHomeTabSelected: (HomeTab) -> Unit = {},
    toastState: AuraToastState = rememberAuraToastState(),
) {
    val colors = AuraTheme.colors

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        bottomBar = {
            AuraBottomBar(
                selected = HomeTab.IONI,
                onTabSelected = onHomeTabSelected,
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
            ) {
                IoniTopBar(
                    selected = uiState.tab,
                    onTabSelected = actions.onTabSelected,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    if (uiState.hasDialogue) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            IoniDialogue(
                                question = uiState.question.orEmpty(),
                                answer = uiState.answer,
                                isThinking = uiState.isThinking,
                            )
                        }
                    } else {
                        IoniWatermark()
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IoniQuestionChips(
                        questions = uiState.questions,
                        onQuestionClick = actions.onQuestionClick,
                    )

                    if (uiState.supportEmail.isNotBlank()) {
                        IoniSupportPill(
                            email = uiState.supportEmail,
                            isExpanded = uiState.isSupportExpanded,
                            onSupportClick = actions.onSupportClick,
                            onEmailClick = { actions.onEmailClick(uiState.supportEmail) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    IoniInputField(
                        value = uiState.input,
                        isSendEnabled = uiState.isSendEnabled,
                        onValueChange = actions.onInputChange,
                        onSendClick = actions.onSendClick,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            AuraToastHost(
                state = toastState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Preview(widthDp = 375, heightDp = 812, showBackground = true, backgroundColor = 0xFF05070A)
@Composable
private fun IoniScreenPreview() {
    AuraTheme {
        IoniScreen(
            uiState = IoniUiState(
                questions = listOf(
                    "What is IO Aura?",
                    "Is IO Aura free?",
                    "What is ION and why collect it?",
                ),
                supportEmail = "support@ioaura.app",
            ),
            actions = IoniActions(),
        )
    }
}
