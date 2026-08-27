package com.aura.feature.ioni.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.R
import com.aura.core.designsystem.theme.AuraTheme

private val BubbleShape = RoundedCornerShape(16.dp)

private val QuestionMaxWidth = 268.dp

private val AnswerMaxWidth = 293.dp

private val ThinkingTextSize = 12.5.sp

private const val WATERMARK_ALPHA = 0.14f

private val WatermarkSize = 220.dp

private const val INDICATOR_ALPHA = 0.80f

private const val ANSWER_ALPHA = 0.80f

@Composable
fun IoniWatermark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        IoniDotPlanet(
            color = AuraTheme.colors.ioniHint,
            size = WatermarkSize,
            alpha = WATERMARK_ALPHA,
            animated = false,
            fade = true,
        )
    }
}

@Composable
fun IoniQuestionBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.actionLabel,
            color = AuraTheme.colors.textPrimary,
            modifier = Modifier
                .widthIn(max = QuestionMaxWidth)
                .clip(BubbleShape)
                .background(AuraTheme.colors.authBorder)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun IoniThinkingRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IoniDotPlanet(color = AuraTheme.colors.textBright, alpha = INDICATOR_ALPHA)

        Text(
            text = stringResource(R.string.ioni_thinking),
            style = AuraTheme.typography.screenSubheading.copy(fontSize = ThinkingTextSize),
            color = AuraTheme.colors.ioniHint,
        )
    }
}

@Composable
fun IoniAnswerBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = AuraTheme.typography.actionLabel,
            color = AuraTheme.colors.textPrimary.copy(alpha = ANSWER_ALPHA),
            modifier = Modifier
                .widthIn(max = AnswerMaxWidth)
                .clip(BubbleShape)
                .background(AuraTheme.colors.ioniAnswer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun IoniDialogue(
    question: String,
    answer: String,
    isThinking: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IoniQuestionBubble(text = question)

        when {
            isThinking -> IoniThinkingRow()
            answer.isNotEmpty() -> IoniAnswerBubble(text = answer)
        }
    }
}
