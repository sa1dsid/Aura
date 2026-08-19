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
        PromoCode("s4", "D2P8-MR61-KT34", PromoCodeKind.SPARK, issuedAt(8, 9, 12), used = false),
        PromoCode("s5", "E7T3-WQ05-NB89", PromoCodeKind.SPARK, issuedAt(7, 20, 55), used = true),
        PromoCode("s6", "F4L9-HD72-XM16", PromoCodeKind.SPARK, issuedAt(7, 14, 5), used = false),
        PromoCode("s7", "G1V6-ZK37-PC42", PromoCodeKind.SPARK, issuedAt(6, 22, 31), used = false),
        PromoCode("s8", "H8N2-QB94-LR58", PromoCodeKind.SPARK, issuedAt(6, 11, 19), used = true),
        PromoCode("s9", "J5R7-TF20-WD63", PromoCodeKind.SPARK, issuedAt(5, 17, 44), used = false),
        PromoCode("s10", "K3M4-YP86-ZQ27", PromoCodeKind.SPARK, issuedAt(5, 8, 3), used = false),
        PromoCode("v1", "VP-7K2M-XQ19", PromoCodeKind.VPN, issuedAt(10, 8, 14), used = false),
        PromoCode("v2", "VP-3D9P-HL62", PromoCodeKind.VPN, issuedAt(9, 21, 2), used = false),
        PromoCode("v3", "VP-6R4T-BW08", PromoCodeKind.VPN, issuedAt(8, 19, 15), used = true),
        PromoCode("v4", "VP-9J1L-KN47", PromoCodeKind.VPN, issuedAt(8, 10, 38), used = false),
        PromoCode("v5", "VP-2C5X-TR93", PromoCodeKind.VPN, issuedAt(7, 16, 21), used = false),
        PromoCode("v6", "VP-8H4Q-MD75", PromoCodeKind.VPN, issuedAt(7, 9, 30), used = true),
        PromoCode("v7", "VP-5B7Z-WF12", PromoCodeKind.VPN, issuedAt(6, 18, 49), used = false),
        PromoCode("v8", "VP-1N6G-PS84", PromoCodeKind.VPN, issuedAt(6, 12, 7), used = false),
        PromoCode("v9", "VP-4T8V-XC50", PromoCodeKind.VPN, issuedAt(5, 20, 26), used = true),
        PromoCode("v10", "VP-7Y3K-QB69", PromoCodeKind.VPN, issuedAt(5, 7, 52), used = false),
    )

    val content = PromoCodesUiState(
        handle = "syrex",
        codes = codes,
    )

    val empty = PromoCodesUiState(handle = "syrex")
}
