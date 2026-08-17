package com.aura.feature.transactions.presentation.preview

import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.feature.transactions.domain.model.TransactionKind
import com.aura.feature.transactions.presentation.TransactionsUiState
import java.util.Calendar

private fun eventAt(day: Int, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        set(2026, Calendar.JULY, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

object TransactionsPreviewData {

    val events = listOf(
        TransactionEvent(
            id = "1",
            timestamp = eventAt(10, 8, 14),
            kind = TransactionKind.ION,
            typeLabel = "ION",
            fieldKey = "source",
            fieldValue = "home_button",
            amount = "+20 ION",
            isCredit = true,
        ),
        TransactionEvent(
            id = "2",
            timestamp = eventAt(10, 8, 14),
            kind = TransactionKind.SPARK,
            typeLabel = "Spark",
            fieldKey = "source",
            fieldValue = "home_button_accrual",
            amount = "+40,000 Spark",
            isCredit = true,
        ),
        TransactionEvent(
            id = "3",
            timestamp = eventAt(10, 13, 2),
            kind = TransactionKind.DATA_SHARE,
            typeLabel = "Data Share",
            fieldKey = "given",
            fieldValue = "1.4 GB · Mobile",
            amount = "+610 ION",
            isCredit = true,
        ),
        TransactionEvent(
            id = "4",
            timestamp = eventAt(10, 21, 47),
            kind = TransactionKind.DATA_SHARE,
            typeLabel = "Data Share",
            fieldKey = "given",
            fieldValue = "2.0 GB · Wi-Fi",
            amount = "+910 ION",
            isCredit = true,
        ),
        TransactionEvent(
            id = "5",
            timestamp = eventAt(9, 21, 2),
            kind = TransactionKind.ION,
            typeLabel = "ION",
            fieldKey = "source",
            fieldValue = "home_button",
            amount = "+20 ION",
            isCredit = true,
        ),
        TransactionEvent(
            id = "6",
            timestamp = eventAt(9, 21, 2),
            kind = TransactionKind.SPARK,
            typeLabel = "Spark",
            fieldKey = "source",
            fieldValue = "home_button_accrual",
            amount = "+40,000 Spark",
            isCredit = true,
        ),
        TransactionEvent(
            id = "7",
            timestamp = eventAt(8, 13, 47),
            kind = TransactionKind.REFERRAL,
            typeLabel = "Referral Bonus",
            fieldKey = "from",
            fieldValue = "@alexk",
            amount = "+1,240 Spark",
            isCredit = true,
        ),
        TransactionEvent(
            id = "8",
            timestamp = eventAt(8, 19, 15),
            kind = TransactionKind.DATA_SHARE,
            typeLabel = "Data Share",
            fieldKey = "given",
            fieldValue = "0.8 GB · Wi-Fi",
            amount = "+340 ION",
            isCredit = true,
        ),
        TransactionEvent(
            id = "9",
            timestamp = eventAt(7, 9, 30),
            kind = TransactionKind.DATA_SHARE,
            typeLabel = "Data Share",
            fieldKey = "given",
            fieldValue = "5.0 GB · Mobile",
            amount = "+2,180 ION",
            isCredit = true,
        ),
        TransactionEvent(
            id = "10",
            timestamp = eventAt(7, 14, 5),
            kind = TransactionKind.EXCHANGE,
            typeLabel = "Exchange",
            fieldKey = "for",
            fieldValue = "promo code",
            amount = "−110 ION",
            isCredit = false,
        ),
        TransactionEvent(
            id = "11",
            timestamp = eventAt(7, 18, 40),
            kind = TransactionKind.EXCHANGE,
            typeLabel = "Exchange",
            fieldKey = "for",
            fieldValue = "promo code",
            amount = "−240,000 Spark",
            isCredit = false,
        ),
    )

    val content = TransactionsUiState(
        handle = "syrex",
        events = events,
    )

    val empty = TransactionsUiState(handle = "syrex")
}
