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

const val PROMO_SECTION_LIMIT = 7

fun List<PromoCode>.sectionCodes(kind: PromoCodeKind): List<PromoCode> = this
    .filter { it.kind == kind }
    .sortedByDescending(PromoCode::issuedAt)
    .take(PROMO_SECTION_LIMIT)
