package com.aura.feature.transactions.domain.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.aura.R

enum class TransactionKind {
    ION,
    SPARK,
    DATA_SHARE,
    REFERRAL,
    EXCHANGE,
}

@Immutable
data class TransactionEvent(
    val id: String,
    val timestamp: Long,
    val kind: TransactionKind,
    val typeLabel: String,
    val fieldKey: String,
    val fieldValue: String,
    val amount: String,
    val isCredit: Boolean,
)

enum class TransactionFilter(
    @field:StringRes val labelRes: Int,
    val kind: TransactionKind?,
) {
    ALL(R.string.tx_filter_all, null),
    ION(R.string.tx_filter_ion, TransactionKind.ION),
    SPARK(R.string.tx_filter_spark, TransactionKind.SPARK),
    DATA_SHARE(R.string.tx_filter_data_share, TransactionKind.DATA_SHARE),
    REFERRAL(R.string.tx_filter_referral, TransactionKind.REFERRAL),
    EXCHANGE(R.string.tx_filter_exchange, TransactionKind.EXCHANGE),
}

fun List<TransactionEvent>.filterBy(filter: TransactionFilter): List<TransactionEvent> =
    filter.kind?.let { kind -> filter { it.kind == kind } } ?: this

const val TRANSACTIONS_LIMIT = 100
