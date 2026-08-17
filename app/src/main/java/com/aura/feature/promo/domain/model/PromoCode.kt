package com.aura.feature.promo.domain.model

import androidx.compose.runtime.Immutable

enum class PromoCodeKind {
    SPARK,
    VPN,
}

@Immutable
data class PromoCode(
    val id: String,
    val code: String,
    val kind: PromoCodeKind,
    val issuedAt: Long,
    val used: Boolean,
)

fun List<PromoCode>.ofKind(kind: PromoCodeKind): List<PromoCode> = filter { it.kind == kind }
