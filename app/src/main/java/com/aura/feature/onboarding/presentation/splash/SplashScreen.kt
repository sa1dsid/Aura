package com.aura.feature.onboarding.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.presentation.components.BrandLogoRow

private val HIGHLIGHT_PATTERN = Regex("""\bok\b|\d[\d,]*""")

@Composable
fun SplashRoute(
    onFinished: (StartDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.startDestination) {
        uiState.startDestination?.let(onFinished)
    }

    SplashScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun SplashScreen(
    uiState: SplashUiState,
    modifier: Modifier = Modifier,
) {
    val colors = AuraTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.authBackground)
            .statusBarsPadding()
            .padding(horizontal = 15.5.dp),
    ) {
        Spacer(Modifier.height(53.dp))

        BrandLogoRow()

        Spacer(Modifier.height(50.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = highlightedLog(uiState.log, uiState.printedLength),
                style = AuraTheme.typography.consoleLine,
                color = colors.authTextMuted,
            )
        }
    }
}

@Composable
private fun highlightedLog(log: String, printedLength: Int): AnnotatedString {
    val ranges = remember(log) { highlightRanges(log) }
    val printed = log.take(printedLength)

    return buildAnnotatedString {
        var cursor = 0
        for (range in ranges) {
            if (range.first >= printed.length) break
            append(printed.substring(cursor, range.first))
            withStyle(SpanStyle(color = AuraTheme.colors.textPrimary)) {
                append(printed.substring(range.first, minOf(range.last, printed.length)))
            }
            cursor = minOf(range.last, printed.length)
        }
        append(printed.substring(cursor))
    }
}

private fun highlightRanges(log: String): List<Highlight> = HIGHLIGHT_PATTERN
    .findAll(log)
    .map { Highlight(it.range.first, it.range.last + 1) }
    .toList()

private data class Highlight(val first: Int, val last: Int)

@Preview(widthDp = 375, heightDp = 813)
@Composable
private fun SplashScreenPreview() {
    AuraTheme {
        SplashScreen(
            uiState = SplashUiState(
                log = "> in the beginning was the ION\n> then came the mesh ... ok\n" +
                    "> 4,210 nodes carry the signal\n> your signal has been expected\n" +
                    "> the network sees you\n> synchronizing your aura\n> opening the gate",
                printedLength = 200,
            ),
        )
    }
}
