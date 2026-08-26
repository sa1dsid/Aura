package com.aura.feature.transactions.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFilterTest {

    private val events = TransactionKind.entries.mapIndexed { index, kind ->
        event(id = index.toString(), kind = kind)
    }

    @Test
    fun `the all filter keeps the log untouched`() {
        assertEquals(events, events.filterBy(TransactionFilter.ALL))
    }

    @Test
    fun `every other filter keeps only its own kind`() {
        TransactionFilter.entries.filter { it.kind != null }.forEach { filter ->
            val kept = events.filterBy(filter)

            assertEquals(listOf(filter.kind), kept.map { it.kind })
        }
    }

    @Test
    fun `the filter row offers one pill per kind plus all`() {
        assertEquals(
            TransactionKind.entries.size + 1,
            TransactionFilter.entries.size,
        )
        assertEquals(TransactionFilter.ALL, TransactionFilter.entries.first())
        assertEquals(
            TransactionKind.entries.toList(),
            TransactionFilter.entries.mapNotNull { it.kind },
        )
    }

    @Test
    fun `a filter without matching events gives an empty log`() {
        val onlyIon = listOf(event(id = "1", kind = TransactionKind.ION))

        assertTrue(onlyIon.filterBy(TransactionFilter.SPARK).isEmpty())
    }

    @Test
    fun `an empty log stays empty under any filter`() {
        TransactionFilter.entries.forEach { filter ->
            assertTrue(emptyList<TransactionEvent>().filterBy(filter).isEmpty())
        }
    }

    private fun event(id: String, kind: TransactionKind) = TransactionEvent(
        id = id,
        timestamp = 1L,
        kind = kind,
        typeLabel = kind.name,
        fieldKey = "source",
        fieldValue = "tap_reward",
        amount = "+20 ION",
        isCredit = true,
    )
}
