package com.aura.feature.terminal

import com.aura.core.api.dto.PromoDto
import com.aura.core.api.dto.TerminalDto
import com.aura.core.api.dto.TransactionDto
import com.aura.feature.terminal.data.remote.TerminalRemoteDataSource

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

    var storedTransactions: List<TransactionDto> = emptyList()

    var storedPromoCodes: List<PromoDto> = emptyList()

    var countersFailure: Throwable? = null

    var transactionsFailure: Throwable? = null

    var promoCodesFailure: Throwable? = null

    var countersCalls = 0
        private set

    var transactionsCalls = 0
        private set

    var promoCodesCalls = 0
        private set

    override suspend fun terminal(): TerminalDto {
        countersCalls++
        countersFailure?.let { throw it }
        return counters
    }

    override suspend fun transactions(): List<TransactionDto> {
        transactionsCalls++
        transactionsFailure?.let { throw it }
        return storedTransactions
    }

    override suspend fun promoCodes(): List<PromoDto> {
        promoCodesCalls++
        promoCodesFailure?.let { throw it }
        return storedPromoCodes
    }
}
