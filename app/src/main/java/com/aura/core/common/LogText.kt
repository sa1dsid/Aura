package com.aura.core.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

private val PLACEHOLDER_PATTERN = Regex("""%(\d+)\$[sd]""")

private const val PUNCTUATION = ":,"

data class LogValue(
    val text: String,
    val style: SpanStyle,
)

fun logLine(
    template: String,
    keyStyle: SpanStyle,
    punctuationStyle: SpanStyle,
    values: List<LogValue>,
): AnnotatedString = buildAnnotatedString {
    var consumedUpTo = 0

    PLACEHOLDER_PATTERN.findAll(template).forEach { match ->
        appendStatic(template.substring(consumedUpTo, match.range.first), keyStyle, punctuationStyle)
        consumedUpTo = match.range.last + 1

        val value = values[match.groupValues[1].toInt() - 1]
        withStyle(value.style) { append(value.text) }
    }

    appendStatic(template.substring(consumedUpTo), keyStyle, punctuationStyle)
}

private fun AnnotatedString.Builder.appendStatic(
    text: String,
    keyStyle: SpanStyle,
    punctuationStyle: SpanStyle,
) {
    if (text.isEmpty()) return

    var runStart = 0
    var runIsPunctuation = text[0] in PUNCTUATION

    text.forEachIndexed { index, symbol ->
        val isPunctuation = symbol in PUNCTUATION
        if (isPunctuation != runIsPunctuation) {
            withStyle(if (runIsPunctuation) punctuationStyle else keyStyle) {
                append(text.substring(runStart, index))
            }
            runStart = index
            runIsPunctuation = isPunctuation
        }
    }

    withStyle(if (runIsPunctuation) punctuationStyle else keyStyle) {
        append(text.substring(runStart))
    }
}
