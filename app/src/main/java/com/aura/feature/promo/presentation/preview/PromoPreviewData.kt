package com.aura.feature.promo.presentation.preview

import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.promo.domain.model.PromoCodeKind
import com.aura.feature.promo.presentation.PromoCodesUiState
import java.util.Calendar

private fun issuedAt(day: Int, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        set(2026, Calendar.JULY, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

object PromoPreviewData {

    val codes = listOf(
        PromoCode("s1", "A8X4-KP92-QW01", PromoCodeKind.SPARK, issuedAt(9, 21, 2), used = false),
        PromoCode("s2", "B3M7-LT48-ZR55", PromoCodeKind.SPARK, issuedAt(9, 18, 40), used = true),
        PromoCode("s3", "C9K1-XN23-VD70", PromoCodeKind.SPARK, issuedAt(8, 13, 47), used = false),
        PromoCode("v1", "VP-7K2M-XQ19", PromoCodeKind.VPN, issuedAt(10, 8, 14), used = false),
        PromoCode("v2", "VP-3D9P-HL62", PromoCodeKind.VPN, issuedAt(9, 21, 2), used = false),
        PromoCode("v3", "VP-6R4T-BW08", PromoCodeKind.VPN, issuedAt(8, 19, 15), used = true),
    )

    val content = PromoCodesUiState(
        handle = "syrex",
        codes = codes,
    )

    val empty = PromoCodesUiState(handle = "syrex")
}
