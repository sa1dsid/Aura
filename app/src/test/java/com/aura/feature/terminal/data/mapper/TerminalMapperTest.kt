package com.aura.feature.terminal.data.mapper

import com.aura.core.api.dto.TransactionDto
import com.aura.feature.transactions.domain.model.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Test

private const val CREATED_AT = "2026-08-25T13:54:33.000Z"

class TerminalMapperTest {

    @Test
    fun `a spark referral bonus lands under the referral filter`() {
        val event = transaction(kind = "referral_bonus", currency = "SPARK", amount = "1240")

        assertEquals(TransactionKind.REFERRAL, event.kind)
    }

    @Test
    fun `a spark exchange lands under the exchange filter`() {
        val event = transaction(kind = "exchange", currency = "SPARK", amount = "-240000")

        assertEquals(TransactionKind.EXCHANGE, event.kind)
    }

    @Test
    fun `a plain spark accrual stays under the spark filter`() {
        val event = transaction(kind = "spark_accrual", currency = "SPARK", amount = "20000")

        assertEquals(TransactionKind.SPARK, event.kind)
    }

    @Test
    fun `a tap reward stays under the ion filter`() {
        val event = transaction(kind = "tap_reward", currency = "ION", amount = "20")

        assertEquals(TransactionKind.ION, event.kind)
    }

    private fun transaction(kind: String, currency: String, amount: String) = TransactionDto(
        id = 1,
        kind = kind,
        currency = currency,
        amount = amount,
        createdAt = CREATED_AT,
    ).toDomain()!!
}
