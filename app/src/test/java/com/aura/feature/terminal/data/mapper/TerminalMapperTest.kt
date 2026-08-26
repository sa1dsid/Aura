package com.aura.feature.terminal.data.mapper

import com.aura.core.api.dto.TransactionDto
import com.aura.feature.promo.domain.model.PromoCodeKind
import com.aura.feature.terminal.Terminal
import com.aura.feature.terminal.promoDto
import com.aura.feature.transactions.domain.model.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `traffic and data share share one filter`() {
        assertEquals(TransactionKind.DATA_SHARE, transaction(kind = "data_share").kind)
        assertEquals(TransactionKind.DATA_SHARE, transaction(kind = "traffic_payout").kind)
    }

    @Test
    fun `a withdrawal is filed as an exchange`() {
        assertEquals(TransactionKind.EXCHANGE, transaction(kind = "withdrawal").kind)
    }

    @Test
    fun `the kind is matched anywhere in the server word and in any case`() {
        assertEquals(TransactionKind.REFERRAL, transaction(kind = "SPARK_REFERRAL_BONUS").kind)
        assertEquals(TransactionKind.EXCHANGE, transaction(kind = "Promo-Exchange").kind)
    }

    @Test
    fun `an unknown kind falls back to the currency`() {
        assertEquals(
            TransactionKind.SPARK,
            transaction(kind = "something_new", currency = "spark").kind,
        )
        assertEquals(
            TransactionKind.ION,
            transaction(kind = "something_new", currency = "ION").kind,
        )
        assertEquals(
            TransactionKind.ION,
            transaction(kind = "something_new", currency = "gold").kind,
        )
    }

    @Test
    fun `every kind carries its own label`() {
        assertEquals("ION", transaction(kind = "tap_reward").typeLabel)
        assertEquals("Spark", transaction(kind = "spark_accrual", currency = "SPARK").typeLabel)
        assertEquals("Data Share", transaction(kind = "data_share").typeLabel)
        assertEquals("Referral", transaction(kind = "referral_bonus").typeLabel)
        assertEquals("Exchange", transaction(kind = "exchange").typeLabel)
    }

    @Test
    fun `every event is filed under the same source field`() {
        val event = transaction(kind = "referral_bonus")

        assertEquals("source", event.fieldKey)
        assertEquals("referral_bonus", event.fieldValue)
    }

    @Test
    fun `a credit gets a plus and grouped digits`() {
        val event = transaction(currency = "SPARK", amount = "40000.000000")

        assertEquals("+40,000 SPARK", event.amount)
        assertTrue(event.isCredit)
    }

    @Test
    fun `a debit gets a minus and loses the raw sign`() {
        val event = transaction(currency = "ION", amount = "-110.000000")

        assertEquals("-110 ION", event.amount)
        assertFalse(event.isCredit)
    }

    @Test
    fun `zero counts as a credit`() {
        val event = transaction(amount = "0.000000")

        assertEquals("+0 ION", event.amount)
        assertTrue(event.isCredit)
    }

    @Test
    fun `the fraction of an amount is cut, not rounded`() {
        assertEquals("+20 ION", transaction(amount = "20.999999").amount)
        assertEquals("-20 ION", transaction(amount = "-20.999999").amount)
    }

    @Test
    fun `the currency is printed the way the screen wants it, in upper case`() {
        assertEquals("+20 ION", transaction(currency = "ion").amount)
        assertEquals("+20 SPARK", transaction(currency = "Spark").amount)
    }

    @Test
    fun `a row the screen cannot read is dropped`() {
        assertNull(dto(createdAt = "not-a-date").toDomain())
        assertNull(dto(amount = "unavailable").toDomain())
    }

    @Test
    fun `a promo campaign that mentions vpn is a vpn code`() {
        assertEquals(PromoCodeKind.VPN, promoDto(campaign = "vpn_month").toDomain()!!.kind)
        assertEquals(PromoCodeKind.VPN, promoDto(campaign = "PLANET_VPN").toDomain()!!.kind)
    }

    @Test
    fun `any other promo campaign is a spark code`() {
        assertEquals(PromoCodeKind.SPARK, promoDto(campaign = "spark_coupon").toDomain()!!.kind)
        assertEquals(PromoCodeKind.SPARK, promoDto(campaign = "").toDomain()!!.kind)
    }

    @Test
    fun `a promo code arrives unused with its code untouched`() {
        val code = promoDto(id = 7, code = "A8X4-KP92-QW01").toDomain()!!

        assertEquals("7", code.id)
        assertEquals("A8X4-KP92-QW01", code.code)
        assertFalse(code.used)
    }

    @Test
    fun `a promo code the screen cannot date is dropped`() {
        assertNull(promoDto(createdAt = "not-a-date").toDomain())
    }

    @Test
    fun `both kinds of row read the same iso timestamp`() {
        val timestamp = Terminal.CREATED_AT.let { dto(createdAt = it).toDomain()!!.timestamp }

        assertEquals(timestamp, promoDto(createdAt = Terminal.CREATED_AT).toDomain()!!.issuedAt)
    }

    private fun dto(
        id: Int = 1,
        kind: String = "tap_reward",
        currency: String = "ION",
        amount: String = "20",
        createdAt: String = CREATED_AT,
    ) = TransactionDto(
        id = id,
        kind = kind,
        currency = currency,
        amount = amount,
        createdAt = createdAt,
    )

    private fun transaction(
        kind: String = "tap_reward",
        currency: String = "ION",
        amount: String = "20",
    ) = dto(kind = kind, currency = currency, amount = amount).toDomain()!!
}
