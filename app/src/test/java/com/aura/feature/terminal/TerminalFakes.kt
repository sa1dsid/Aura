package com.aura.feature.terminal

import com.aura.core.api.dto.PromoDto
import com.aura.core.api.dto.TerminalDto
import com.aura.core.api.dto.TransactionDto
import com.aura.feature.promo.data.remote.PromoCodesRemoteDataSource
import com.aura.feature.terminal.data.remote.TerminalRemoteDataSource
import com.aura.feature.transactions.data.remote.TransactionsRemoteDataSource

internal fun terminalDto(transactionsNew: Int = 0, promoCodesNew: Int = 0) = TerminalDto(
    transactionsNew = transactionsNew,
    promoCodesNew = promoCodesNew,
)

internal fun transactionDto(
    id: Int = 1,
    kind: String = Terminal.KIND_TAP_REWARD,
    currency: String = Terminal.CURRENCY_ION,
    amount: String = "20.000000",
    createdAt: String = Terminal.CREATED_AT,
) = TransactionDto(
    id = id,
    kind = kind,
    currency = currency,
    amount = amount,
    createdAt = createdAt,
)

internal fun promoDto(
    id: Int = 1,
    code: String = "A8X4-KP92-QW01",
    campaign: String = Terminal.CAMPAIGN_SPARK,
    createdAt: String = Terminal.CREATED_AT,
) = PromoDto(
    id = id,
    code = code,
    campaign = campaign,
    createdAt = createdAt,
)

internal class FakeTerminalRemoteDataSource : TerminalRemoteDataSource {

    var counters: TerminalDto = terminalDto()

    var failure: Throwable? = null

    var calls = 0
        private set

    override suspend fun terminal(): TerminalDto {
        calls++
        failure?.let { throw it }
        return counters
    }
}

internal class FakeTransactionsRemoteDataSource : TransactionsRemoteDataSource {

    var stored: List<TransactionDto> = emptyList()

    var failure: Throwable? = null

    var calls = 0
        private set

    override suspend fun transactions(): List<TransactionDto> {
        calls++
        failure?.let { throw it }
        return stored
    }
}

internal class FakePromoCodesRemoteDataSource : PromoCodesRemoteDataSource {

    var stored: List<PromoDto> = emptyList()

    var failure: Throwable? = null

    var calls = 0
        private set

    override suspend fun promoCodes(): List<PromoDto> {
        calls++
        failure?.let { throw it }
        return stored
    }
}
