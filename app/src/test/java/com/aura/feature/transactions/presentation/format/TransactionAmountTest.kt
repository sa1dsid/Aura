package com.aura.feature.transactions.presentation.format

import com.aura.R
import com.aura.feature.transactions.domain.model.TransactionEvent
import com.aura.feature.transactions.domain.model.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionAmountTest {

    @Test
    fun `a credit is printed with a plus and grouped digits`() {
        assertEquals("+40,000 Spark", event(amount = 40_000).formatAmount("Spark"))
    }

    @Test
    fun `a debit is printed with a minus`() {
        assertEquals(
            "-240,000 Spark",
            event(amount = 240_000, isCredit = false).formatAmount("Spark"),
        )
    }

    @Test
    fun `zero is printed as a credit`() {
        assertEquals("+0 ION", event(amount = 0).formatAmount("ION"))
    }

    @Test
    fun `spark and ion get the units the design asks for`() {
        assertEquals(R.string.unit_spark, "SPARK".currencyUnitRes())
        assertEquals(R.string.unit_spark, "spark".currencyUnitRes())
        assertEquals(R.string.unit_ion, "ION".currencyUnitRes())
        assertEquals(R.string.unit_ion, "ion".currencyUnitRes())
    }

    @Test
    fun `a currency the app does not know has no unit of its own`() {
        assertNull("gold".currencyUnitRes())
    }

    private fun event(amount: Long, isCredit: Boolean = true) = TransactionEvent(
        id = "1",
        timestamp = 1L,
        kind = TransactionKind.ION,
        detail = "tap_reward",
        amount = amount,
        currency = "ION",
        isCredit = isCredit,
    )
}
