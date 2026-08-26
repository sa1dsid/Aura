package com.aura.feature.transactions.data.mapper

import com.aura.feature.terminal.transactionDto
import com.aura.feature.transactions.domain.model.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionsMapperTest {

    @Test
    fun `a spark referral bonus lands under the referral filter`() {
        assertEquals(
            TransactionKind.REFERRAL,
            event(kind = "referral_bonus", currency = "SPARK", amount = "1240").kind,
        )
    }

    @Test
    fun `a spark exchange lands under the exchange filter`() {
        assertEquals(
            TransactionKind.EXCHANGE,
            event(kind = "exchange", currency = "SPARK", amount = "-240000").kind,
        )
    }

    @Test
    fun `a plain spark accrual stays under the spark filter`() {
        assertEquals(
            TransactionKind.SPARK,
            event(kind = "spark_accrual", currency = "SPARK", amount = "20000").kind,
        )
    }

    @Test
    fun `a tap reward stays under the ion filter`() {
        assertEquals(TransactionKind.ION, event(kind = "tap_reward", amount = "20").kind)
    }

    @Test
    fun `traffic and data share share one filter`() {
        assertEquals(TransactionKind.DATA_SHARE, event(kind = "data_share").kind)
        assertEquals(TransactionKind.DATA_SHARE, event(kind = "traffic_payout").kind)
    }

    @Test
    fun `a withdrawal is filed as an exchange`() {
        assertEquals(TransactionKind.EXCHANGE, event(kind = "withdrawal").kind)
    }

    @Test
    fun `the kind is matched anywhere in the server word and in any case`() {
        assertEquals(TransactionKind.REFERRAL, event(kind = "SPARK_REFERRAL_BONUS").kind)
        assertEquals(TransactionKind.EXCHANGE, event(kind = "Promo-Exchange").kind)
    }

    @Test
    fun `an unknown kind falls back to the currency`() {
        assertEquals(TransactionKind.SPARK, event(kind = "new", currency = "spark").kind)
        assertEquals(TransactionKind.ION, event(kind = "new", currency = "ION").kind)
        assertEquals(TransactionKind.ION, event(kind = "new", currency = "gold").kind)
    }

    @Test
    fun `the raw server kind is kept as the detail of the log line`() {
        assertEquals("referral_bonus", event(kind = "referral_bonus").detail)
    }

    @Test
    fun `the currency is kept exactly as the server sent it`() {
        assertEquals("spark", event(currency = "spark").currency)
    }

    @Test
    fun `a credit keeps its magnitude and is marked as one`() {
        val credit = event(currency = "SPARK", amount = "40000.000000")

        assertEquals(40_000L, credit.amount)
        assertTrue(credit.isCredit)
    }

    @Test
    fun `a debit loses its sign but is marked as one`() {
        val debit = event(amount = "-110.000000")

        assertEquals(110L, debit.amount)
        assertFalse(debit.isCredit)
    }

    @Test
    fun `zero counts as a credit`() {
        assertTrue(event(amount = "0.000000").isCredit)
    }

    @Test
    fun `the fraction of an amount is cut, not rounded`() {
        assertEquals(20L, event(amount = "20.999999").amount)
        assertEquals(20L, event(amount = "-20.999999").amount)
    }

    @Test
    fun `a row the screen cannot read is dropped`() {
        assertNull(transactionDto(createdAt = "not-a-date").toDomain())
        assertNull(transactionDto(amount = "unavailable").toDomain())
    }

    @Test
    fun `the server id becomes the row key`() {
        assertEquals("7", transactionDto(id = 7).toDomain()!!.id)
    }

    private fun event(
        kind: String = "tap_reward",
        currency: String = "ION",
        amount: String = "20",
    ) = transactionDto(kind = kind, currency = currency, amount = amount).toDomain()!!
}
