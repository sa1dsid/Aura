package com.aura.feature.onboarding

internal object Server {

    const val PENDING = "pending"
    const val APPLIED = "applied"
    const val SKIPPED = "skipped"

    fun user(
        id: Int = 39,
        email: String = "smoke@auratest.dev",
        displayName: String = "smoke",
        authMethods: List<String> = listOf("email"),
        promoCode: String = "3ZBCZ9MA",
        inviteDecision: String = PENDING,
        giftPopupSeen: Boolean = false,
        bonusReservedIon: Long = 3_000L,
        pushEnabled: Boolean = true,
    ): String = """
        {"id":$id,"email":"$email","display_name":"$displayName",
        "auth_methods":${authMethods.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
        "promo_code":"$promoCode","invite_decision":"$inviteDecision",
        "gift_popup_seen":$giftPopupSeen,"bonus_teaser_seen":false,"push_enabled":$pushEnabled,
        "bonus_reserved_ion":$bonusReservedIon,"accrued_ion":0,"withdrawable_ion":0,
        "spark_balance":"0.000000","tap_count":0,"country":"EE",
        "created_at":"2026-08-24T16:57:17.519755Z"}
    """.trimIndent()

    fun token(
        accessToken: String = "header.payload.signature",
        expiresIn: Int = 604_800,
        isNewAccount: Boolean = false,
        user: String = user(),
    ): String = """
        {"access_token":"$accessToken","token_type":"bearer","expires_in":$expiresIn,
        "is_new_account":$isNewAccount,"user":$user}
    """.trimIndent()

    fun verificationPending(
        email: String = "smoke@auratest.dev",
        expiresIn: Int = 600,
    ): String = """
        {"message":"Confirmation code sent","email":"$email","expires_in":$expiresIn}
    """.trimIndent()

    fun message(text: String = "If the account needs verification, a code was sent"): String =
        """{"message":"$text"}"""

    fun inviteState(decision: String = APPLIED, appliedCode: String? = "IDF46VS0"): String = """
        {"decision":"$decision","applied_code":${appliedCode?.let { "\"$it\"" } ?: "null"},
        "source":"manual","personal_code":"K69VL9R7",
        "personal_url":"https://io-aura.example/invite?code=K69VL9R7",
        "share_text":"Join my node on IO Aura"}
    """.trimIndent()

    fun giftPopupSeen(): String =
        """{"gift_popup_seen":true,"bonus_teaser_should_blink":true}"""

    fun mesh(nodesOnline: Int = 12_048, cities: List<String> = listOf("Tallinn")): String = """
        {"glowing_cities":${cities.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
        "nodes_online":$nodesOnline,"nodes_online_stale":false,
        "nodes_online_updated_at":"2026-08-24T16:42:08.738817Z"}
    """.trimIndent()

    fun detail(text: String): String = """{"detail":"$text"}"""

    fun fieldError(field: String, type: String): String = """
        {"detail":[{"type":"$type","loc":["body","$field"],"msg":"invalid"}]}
    """.trimIndent()
}
