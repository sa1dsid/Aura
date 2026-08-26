package com.aura.feature.transactions.presentation.format

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aura.R
import com.aura.feature.home.presentation.format.formatGrouped
import com.aura.feature.transactions.domain.model.TransactionEvent

private const val CURRENCY_SPARK = "SPARK"

private const val CURRENCY_ION = "ION"

@StringRes
fun String.currencyUnitRes(): Int? = when (uppercase()) {
    CURRENCY_SPARK -> R.string.unit_spark
    CURRENCY_ION -> R.string.unit_ion
    else -> null
}

fun TransactionEvent.formatAmount(unit: String): String {
    val sign = if (isCredit) "+" else "-"
    return "$sign${amount.formatGrouped()} $unit"
}

@Composable
fun TransactionEvent.formatAmount(): String {
    val unitRes = currency.currencyUnitRes()
    return formatAmount(unitRes?.let { stringResource(it) } ?: currency.uppercase())
}
