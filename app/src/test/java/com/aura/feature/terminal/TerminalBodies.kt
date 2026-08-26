package com.aura.feature.terminal

internal object Terminal {

    const val CREATED_AT = "2026-08-25T13:54:33.000Z"

    const val KIND_TAP_REWARD = "tap_reward"
    const val KIND_SPARK_ACCRUAL = "spark_accrual"
    const val KIND_REFERRAL_BONUS = "referral_bonus"
    const val KIND_EXCHANGE = "exchange"
    const val KIND_DATA_SHARE = "data_share"

    const val CURRENCY_ION = "ION"
    const val CURRENCY_SPARK = "SPARK"

    const val CAMPAIGN_SPARK = "spark_coupon"
    const val CAMPAIGN_VPN = "vpn_month"

    fun counters(transactionsNew: Int = 0, promoCodesNew: Int = 0): String = """
        {"transactions_new":$transactionsNew,"promo_codes_new":$promoCodesNew,
        "feature_flags":{"data_share":false,"traffic_withdrawals":false,"vpn_code":false}}
    """.trimIndent()

    fun transaction(
        id: Int = 1,
        kind: String = KIND_TAP_REWARD,
        currency: String = CURRENCY_ION,
        amount: String = "20.000000",
        createdAt: String = CREATED_AT,
    ): String = """
        {"id":$id,"kind":"$kind","currency":"$currency",
        "amount":"$amount","created_at":"$createdAt"}
    """.trimIndent()

    fun promo(
        id: Int = 1,
        code: String = "A8X4-KP92-QW01",
        campaign: String = CAMPAIGN_SPARK,
        createdAt: String = CREATED_AT,
    ): String = """
        {"id":$id,"code":"$code","campaign":"$campaign","created_at":"$createdAt"}
    """.trimIndent()

    fun list(rows: List<String>): String = rows.joinToString(prefix = "[", postfix = "]")

    fun isoAt(day: Int, hour: Int = 10, minute: Int = 0): String =
        "2026-08-%02dT%02d:%02d:00.000Z".format(day, hour, minute)

    fun isoOf(index: Int): String = isoAt(day = 1 + index / MINUTES, minute = index % MINUTES)

    private const val MINUTES = 60
}
