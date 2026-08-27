package com.aura.feature.promo.data.mapper

import com.aura.core.api.dto.PromoDto
import com.aura.core.common.parseIsoMillis
import com.aura.feature.promo.domain.model.PromoCode
import com.aura.feature.promo.domain.model.PromoCodeKind

private const val CAMPAIGN_VPN = "vpn"

fun PromoDto.toDomain(): PromoCode? {
    val issuedAt = createdAt.parseIsoMillis() ?: return null

    return PromoCode(
        id = id.toString(),
        code = code,
        kind = if (campaign.contains(CAMPAIGN_VPN, ignoreCase = true)) {
            PromoCodeKind.VPN
        } else {
            PromoCodeKind.SPARK
        },
        issuedAt = issuedAt,
        used = false,
    )
}
