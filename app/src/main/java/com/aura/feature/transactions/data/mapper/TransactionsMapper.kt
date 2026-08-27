package com.aura.feature.transactions.data.mapper

import com.aura.core.api.dto.TransactionDto
import com.aura.core.common.parseIsoMillis
import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.feature.transactions.domain.model.TransactionKind
import kotlin.math.abs

private const val CURRENCY_SPARK = "SPARK"

fun TransactionDto.toDomain(): TransactionEvent? {
    val timestamp = createdAt.parseIsoMillis() ?: return null
    val value = amount.toDoubleOrNull() ?: return null

    return TransactionEvent(
        id = id.toString(),
        timestamp = timestamp,
        kind = resolveKind(),
        detail = kind,
        amount = abs(value).toLong(),
        currency = currency,
        isCredit = value >= 0,
    )
}

private fun TransactionDto.resolveKind(): TransactionKind = when {
    kind.contains("referral", ignoreCase = true) -> TransactionKind.REFERRAL
    kind.contains("data_share", ignoreCase = true) -> TransactionKind.DATA_SHARE
    kind.contains("traffic", ignoreCase = true) -> TransactionKind.DATA_SHARE
    kind.contains("exchange", ignoreCase = true) -> TransactionKind.EXCHANGE
    kind.contains("withdraw", ignoreCase = true) -> TransactionKind.EXCHANGE
    currency.equals(CURRENCY_SPARK, ignoreCase = true) -> TransactionKind.SPARK
    else -> TransactionKind.ION
}
