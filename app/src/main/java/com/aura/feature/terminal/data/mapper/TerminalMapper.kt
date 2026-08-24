package com.aura.feature.terminal.data.mapper

import com.aura.core.api.dto.PromoDto
import com.aura.core.api.dto.TransactionDto
import com.aura.core.common.parseIsoMillis
import com.aura.feature.home.presentation.format.formatGrouped
import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.promo.domain.model.PromoCodeKind
import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.feature.transactions.domain.model.TransactionKind

private const val CURRENCY_SPARK = "SPARK"

private const val FIELD_SOURCE = "source"

private const val CAMPAIGN_VPN = "vpn"

private const val WORD_SEPARATOR = '_'

fun TransactionDto.toDomain(): TransactionEvent? {
    val timestamp = createdAt.parseIsoMillis() ?: return null
    val value = amount.toDoubleOrNull() ?: return null
    val transactionKind = resolveKind()

    return TransactionEvent(
        id = id.toString(),
        timestamp = timestamp,
        kind = transactionKind,
        typeLabel = transactionKind.label(),
        fieldKey = FIELD_SOURCE,
        fieldValue = kind,
        amount = value.formatAmount(currency),
        isCredit = value >= 0,
    )
}

fun PromoDto.toDomain(): PromoCode? {
    val issuedAt = createdAt.parseIsoMillis() ?: return null

    return PromoCode(
        id = id.toString(),
        code = code,
        kind = if (campaign.contains(CAMPAIGN_VPN, ignoreCase = true)) {
            PromoCodeKind.VPN
        } else {
            PromoCodeKind.SPARK
        },
        issuedAt = issuedAt,
        used = false,
    )
}

private fun TransactionDto.resolveKind(): TransactionKind = when {
    currency.equals(CURRENCY_SPARK, ignoreCase = true) -> TransactionKind.SPARK
    kind.contains("referral", ignoreCase = true) -> TransactionKind.REFERRAL
    kind.contains("data_share", ignoreCase = true) -> TransactionKind.DATA_SHARE
    kind.contains("traffic", ignoreCase = true) -> TransactionKind.DATA_SHARE
    kind.contains("exchange", ignoreCase = true) -> TransactionKind.EXCHANGE
    kind.contains("withdraw", ignoreCase = true) -> TransactionKind.EXCHANGE
    else -> TransactionKind.ION
}

private fun TransactionKind.label(): String = name
    .split(WORD_SEPARATOR)
    .joinToString(" ") { word -> word.lowercase().replaceFirstChar(Char::uppercaseChar) }

private fun Double.formatAmount(currency: String): String {
    val sign = if (this >= 0) "+" else "-"
    val magnitude = kotlin.math.abs(this).toLong()
    return "$sign${magnitude.formatGrouped()} ${currency.uppercase()}"
}
