package com.aura.feature.account.data.mapper

import com.aura.feature.account.data.remote.dto.LegalLinksDto
import com.aura.feature.account.domain.model.AccountProfile
import com.aura.feature.account.domain.model.LegalLinks
import com.aura.feature.onboarding.domain.model.Account

fun Account.toProfile() = AccountProfile(
    handle = handle,
    email = email,
    authProvider = authProvider,
)

fun LegalLinksDto.toDomain() = LegalLinks(
    termsUrl = termsUrl,
    privacyUrl = privacyUrl,
)
