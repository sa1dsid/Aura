package com.aura.core.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiTestServer {

    private val server = MockWebServer()

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    val api: AuraApi = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AuraApi::class.java)

    fun enqueue(code: Int, body: String = "") {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )
    }

    fun take(): RecordedRequest = server.takeRequest()

    fun sentBody(): String = take().body.readUtf8()

    fun shutdown() = server.shutdown()
}

object Bodies {

    const val USER = """
        {"id":39,"email":"smoke@auratest.dev","display_name":"smoke",
        "auth_methods":["email"],"promo_code":"3ZBCZ9MA","invite_decision":"pending",
        "gift_popup_seen":false,"bonus_teaser_seen":false,"push_enabled":true,
        "bonus_reserved_ion":3000,"accrued_ion":0,"withdrawable_ion":0,
        "spark_balance":"0.000000","tap_count":0,"country":"EE",
        "created_at":"2026-08-24T16:57:17.519755Z"}
    """

    const val TOKEN = """
        {"access_token":"header.payload.signature","token_type":"bearer","expires_in":604800,
        "is_new_account":true,"user":$USER}
    """

    const val VERIFICATION_PENDING = """
        {"message":"Confirmation code sent","email":"a@b.dev","expires_in":600}
    """

    const val INVITE_STATE = """
        {"decision":"applied","applied_code":"IDF46VS0","source":"manual",
        "personal_code":"K69VL9R7","personal_url":"https://io-aura.example/invite?code=K69VL9R7",
        "share_text":"Join my node on IO Aura: https://io-aura.example/invite?code=K69VL9R7"}
    """

    const val DASHBOARD = """
        {"accrued_ion":20,"available_to_withdraw_ion":0,"reserved_bonus_ion":3000,
        "spark_balance":"17.12962966666666666666666667",
        "spark_coupon_campaign":{"issued":0,"limit":4,"completed":false},"tap_count":1,
        "cooldown_available_at":"2026-08-25T05:10:18.767918Z",
        "node":{"tier":"idle","progress_metric":"own_taps","progress_current":1,
        "progress_target":6,"rate_multiplier_percent":100,"spark_referral_percent":0,
        "ion_referral_percent_stage_2":0,"active_friends":0},
        "bonus":{"completed":0,"total":4,"signal_lock":1,"network_sync":0,"full_uplink":1,
        "data_share_gb":0,"data_share_soon":true},
        "gift_popup_should_show":false,"bonus_teaser_should_blink":true,"unread_news":0,
        "battery_optimization":{"should_show":true,"declined_at":null,
        "optimization_disabled":false},
        "ioni_state":"coming","ioni_last_completed_tap":null}
    """

    const val TAP_RUNNING = """
        {"session_id":"f19b9aea-ef96-4972-ac64-12c4e297c7d0","status":"running","tap_count":0,
        "accrued_ion":0,"spark_balance":"0.000000","cooldown_available_at":null,
        "spark_window_rate":0}
    """

    const val TAP_COMPLETED = """
        {"session_id":"f19b9aea-ef96-4972-ac64-12c4e297c7d0","status":"completed","tap_count":1,
        "accrued_ion":20,"spark_balance":"0.000000",
        "cooldown_available_at":"2026-08-25T05:10:18.767918Z","spark_window_rate":20000}
    """

    const val PING = """
        {"id":16,"ip":"94.183.170.97","operator":"MegaFon","location":"Tallinn",
        "connection":"wifi","protocol":"IPv4","vpn":false,"ping_ms":"23.500",
        "jitter_ms":"3.100","packet_loss_pct":"0.000","download_mbps":"55.200",
        "upload_mbps":"12.300","score":"3.6","measured_at":"2026-08-24T16:57:20.470684Z"}
    """

    const val NETWORK_STATE = """
        {"ip":"94.183.170.97","operator":"MegaFon","location":"Tallinn","connection":"wifi",
        "protocol":"IPv4","vpn":false}
    """

    const val NETWORK_SUMMARY = """
        {"ip":"94.183.170.97","operator":"MegaFon","location":"Tallinn","connection":"wifi",
        "protocol":"IPv4","vpn":false,"last_tested_at":"2026-08-24T16:57:20.470684Z",
        "ping_ms":"23.500","jitter_ms":"3.100","packet_loss_pct":"0.000"}
    """

    const val NODES = """
        {"friends_joined":0,"active_friends":0,"tier":"idle","next_threshold":6,
        "more_for_next_tier":null,
        "node_status":{"tier":"idle","progress_metric":"own_taps","progress_current":1,
        "progress_target":6,"rate_multiplier_percent":100,"spark_referral_percent":0,
        "ion_referral_percent_stage_2":0,"active_friends":null},
        "spark_referral_percent":0,"ion_referral_percent_stage_2":0,
        "earned_from_referrals":{"spark":"0.000000","ion":0},"friends":[]}
    """

    const val CONFIG = """
        {"terms_url":"https://io-aura.example/terms",
        "privacy_url":"https://io-aura.example/privacy",
        "feature_flags":{"data_share":false,"traffic_withdrawals":false,"vpn_code":false},
        "social_links":[{"kind":"telegram","url":""},{"kind":"discord","url":""},
        {"kind":"x","url":""},{"kind":"instagram","url":""},{"kind":"youtube","url":""},
        {"kind":"tiktok","url":""}],
        "ioni":{"support_email":"","assistant_enabled":false,"ai_released":false}}
    """

    const val TERMINAL = """
        {"transactions_new":0,"promo_codes_new":0,
        "feature_flags":{"data_share":false,"traffic_withdrawals":false,"vpn_code":false}}
    """

    const val TRANSACTIONS = """
        [{"id":95,"kind":"tap_reward","currency":"ION","amount":"20.000000",
        "created_at":"2026-08-24T17:06:43.457956Z"}]
    """

    const val MESH = """
        {"glowing_cities":["Eygelshoven","Rostov-na-Donu","Tallinn"],"nodes_online":2,
        "nodes_online_stale":true,"nodes_online_updated_at":"2026-08-24T16:42:08.738817Z"}
    """
}
