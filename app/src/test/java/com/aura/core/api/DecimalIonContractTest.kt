package com.aura.core.api

import com.aura.core.api.dto.EmailVerificationConfirmDto
import com.aura.core.api.dto.TapFinishDto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class DecimalIonContractTest {

    private val server = ApiTestServer()

    private val api = server.api

    @After
    fun tearDown() = server.shutdown()

    private val userWithDecimalIon = """
        {"id":271,"email":"deep.probe9@auratest.dev","display_name":"deep.probe9",
        "auth_methods":["email"],"promo_code":"B2GEIV57","invite_decision":"pending",
        "gift_popup_seen":false,"bonus_teaser_seen":false,"push_enabled":true,
        "bonus_reserved_ion":"3000.000000","accrued_ion":"0.000000",
        "withdrawable_ion":"0.000000","spark_balance":"0.000000","tap_count":0,
        "country":"RU","created_at":"2026-08-24T21:06:13.009862Z"}
    """

    @Test
    fun `email confirmation survives the decimal string ion the server sends now`() = runTest {
        server.enqueue(
            200,
            """{"access_token":"a.b.c","token_type":"bearer","expires_in":604800,
            "is_new_account":true,"user":$userWithDecimalIon}""",
        )

        val token = api.confirmEmailVerification(
            EmailVerificationConfirmDto("a@b.dev", "482913"),
        )

        assertEquals(3000L, token.user.bonusReservedIon.toLong())
        assertEquals(0L, token.user.accruedIon.toLong())
        assertEquals(0L, token.user.withdrawableIon.toLong())
    }

    @Test
    fun `current user survives the decimal string ion`() = runTest {
        server.enqueue(200, userWithDecimalIon)

        val user = api.currentUser()

        assertEquals(3000L, user.bonusReservedIon.toLong())
    }

    @Test
    fun `dashboard survives the decimal string ion`() = runTest {
        server.enqueue(
            200,
            """{"accrued_ion":"20.000000","available_to_withdraw_ion":"0.000000",
            "reserved_bonus_ion":"3000.000000","spark_balance":"17.129630",
            "spark_coupon_campaign":{"issued":0,"limit":4,"completed":false},"tap_count":1,
            "cooldown_available_at":"2026-08-25T05:10:18.767918Z",
            "node":{"tier":"idle","progress_metric":"own_taps","progress_current":1,
            "progress_target":6,"rate_multiplier_percent":100,"spark_referral_percent":0,
            "ion_referral_percent_stage_2":0,"active_friends":0},
            "bonus":{"completed":0,"total":4,"signal_lock":1,"network_sync":0,"full_uplink":1,
            "data_share_gb":0,"data_share_soon":true},
            "gift_popup_should_show":false,"bonus_teaser_should_blink":true,"unread_news":0,
            "battery_optimization":{"should_show":true,"declined_at":null,
            "optimization_disabled":false},"ioni_state":"coming",
            "ioni_last_completed_tap":null}""",
        )

        val dashboard = api.dashboard()

        assertEquals(20L, dashboard.accruedIon.toLong())
        assertEquals(3000L, dashboard.reservedBonusIon.toLong())
    }

    @Test
    fun `tap finish survives the decimal string ion`() = runTest {
        server.enqueue(
            200,
            """{"session_id":"abc","status":"completed","tap_count":1,
            "accrued_ion":"20.000000","spark_balance":"0.000000",
            "cooldown_available_at":"2026-08-25T05:10:18.767918Z","spark_window_rate":20000}""",
        )

        val finished = api.finishTap("abc", TapFinishDto(interrupted = false))

        assertEquals(20L, finished.accruedIon.toLong())
    }

    @Test
    fun `nodes survives the decimal string ion on friends and referrals`() = runTest {
        server.enqueue(
            200,
            """{"friends_joined":1,"active_friends":1,"tier":"active_signal",
            "next_threshold":6,"more_for_next_tier":1,
            "node_status":{"tier":"active_signal","progress_metric":"own_taps",
            "progress_current":6,"progress_target":6,"rate_multiplier_percent":110,
            "spark_referral_percent":10,"ion_referral_percent_stage_2":2,"active_friends":1},
            "spark_referral_percent":10,"ion_referral_percent_stage_2":2,
            "earned_from_referrals":{"spark":"120.000000","ion":"40.000000"},
            "friends":[{"id":7,"display_name":"friend","status":"earning",
            "own_spark":"500.000000","own_ion":"20.000000","tap_count":1}]}""",
        )

        val nodes = api.nodes()

        assertEquals(40L, nodes.earnedFromReferrals.ion.toLong())
        assertEquals(20L, nodes.friends.single().ownIon.toLong())
    }
}
