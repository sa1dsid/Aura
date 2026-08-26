package com.aura.feature.transactions.domain.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.aura.R

enum class TransactionKind(
    @field:StringRes val labelRes: Int,
    @field:StringRes val fieldKeyRes: Int,
) {
    ION(R.string.tx_type_ion, R.string.tx_log_key_source),
    SPARK(R.string.tx_type_spark, R.string.tx_log_key_source),
    DATA_SHARE(R.string.tx_type_data_share, R.string.tx_log_key_given),
    REFERRAL(R.string.tx_type_referral, R.string.tx_log_key_from),
    EXCHANGE(R.string.tx_type_exchange, R.string.tx_log_key_for),
}

@Immutable
data class TransactionEvent(
    val id: String,
    val timestamp: Long,
    val kind: TransactionKind,
    val detail: String,
    val amount: Long,
    val currency: String,
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
