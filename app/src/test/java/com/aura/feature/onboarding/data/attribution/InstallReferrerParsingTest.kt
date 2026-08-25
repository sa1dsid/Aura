package com.aura.feature.onboarding.data.attribution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val CODE = "SYREX482"

@RunWith(RobolectricTestRunner::class)
class InstallReferrerParsingTest {

    @Test
    fun `a store install with no campaign carries no code`() {
        assertNull(parseInviteCode("utm_source=google-play&utm_medium=organic"))
    }

    @Test
    fun `the code parameter is read`() {
        assertEquals(CODE, parseInviteCode("code=$CODE"))
    }

    @Test
    fun `the invite parameter is read`() {
        assertEquals(CODE, parseInviteCode("invite=$CODE"))
    }

    @Test
    fun `the invite underscore code parameter is read`() {
        assertEquals(CODE, parseInviteCode("invite_code=$CODE"))
    }

    @Test
    fun `the code survives the campaign parameters around it`() {
        assertEquals(
            CODE,
            parseInviteCode("utm_source=google-play&code=$CODE&utm_medium=organic"),
        )
    }

    @Test
    fun `the code parameter wins over the invite one`() {
        assertEquals(CODE, parseInviteCode("invite=AAAA1111&code=$CODE"))
    }

    @Test
    fun `a referrer that is nothing but the code is taken as the code`() {
        assertEquals(CODE, parseInviteCode(CODE))
    }

    @Test
    fun `a percent encoded value is decoded`() {
        assertEquals("SYREX 482", parseInviteCode("code=SYREX%20482"))
    }

    @Test
    fun `a parameter without a name is skipped`() {
        assertNull(parseInviteCode("=$CODE"))
    }

    @Test
    fun `an empty referrer reads as an empty code rather than none`() {
        assertEquals("", parseInviteCode(""))
    }

    @Test
    fun `a percent encoded separator is not treated as a parameter`() {
        assertEquals("code%3D$CODE", parseInviteCode("code%3D$CODE"))
    }
}
