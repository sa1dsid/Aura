package com.aura.feature.promo.data.mapper

import com.aura.core.common.parseIsoMillis
import com.aura.feature.promo.domain.model.PromoCodeKind
import com.aura.feature.terminal.Terminal
import com.aura.feature.terminal.promoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PromoMapperTest {

    @Test
    fun `a campaign that mentions vpn is a vpn code`() {
        assertEquals(PromoCodeKind.VPN, promoDto(campaign = "vpn_month").toDomain()!!.kind)
        assertEquals(PromoCodeKind.VPN, promoDto(campaign = "PLANET_VPN").toDomain()!!.kind)
    }

    @Test
    fun `any other campaign is a spark code`() {
        assertEquals(PromoCodeKind.SPARK, promoDto(campaign = "spark_coupon").toDomain()!!.kind)
        assertEquals(PromoCodeKind.SPARK, promoDto(campaign = "").toDomain()!!.kind)
    }

    @Test
    fun `a code arrives unused with its text untouched`() {
        val code = promoDto(id = 7, code = "A8X4-KP92-QW01").toDomain()!!

        assertEquals("7", code.id)
        assertEquals("A8X4-KP92-QW01", code.code)
        assertFalse(code.used)
    }

    @Test
    fun `the issue date is read from the iso timestamp`() {
        assertEquals(
            Terminal.CREATED_AT.parseIsoMillis(),
            promoDto(createdAt = Terminal.CREATED_AT).toDomain()!!.issuedAt,
        )
    }

    @Test
    fun `a code the screen cannot date is dropped`() {
        assertNull(promoDto(createdAt = "not-a-date").toDomain())
    }
}
