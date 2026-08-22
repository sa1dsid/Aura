package com.aura.feature.promo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PromoSectionCapTest {

    @Test
    fun `keeps the seven newest codes of the kind and drops the older ones`() {
        val codes = (1..PROMO_SECTION_LIMIT + 4).map { day -> code("s$day", PromoCodeKind.SPARK, day) }

        val section = codes.sectionCodes(PromoCodeKind.SPARK)

        assertEquals(PROMO_SECTION_LIMIT, section.size)
        assertEquals("s11", section.first().id)
        assertEquals("s5", section.last().id)
    }

    @Test
    fun `leaves out the codes of the other kind`() {
        val codes = listOf(
            code("s1", PromoCodeKind.SPARK, day = 1),
            code("v1", PromoCodeKind.VPN, day = 2),
            code("s2", PromoCodeKind.SPARK, day = 3),
        )

        assertEquals(listOf("s2", "s1"), codes.sectionCodes(PromoCodeKind.SPARK).map { it.id })
        assertEquals(listOf("v1"), codes.sectionCodes(PromoCodeKind.VPN).map { it.id })
    }

    private fun code(id: String, kind: PromoCodeKind, day: Int): PromoCode = PromoCode(
        id = id,
        code = "CODE-$id",
        kind = kind,
        issuedAt = 1_754_000_000_000 + day * 86_400_000L,
        used = false,
    )
}
