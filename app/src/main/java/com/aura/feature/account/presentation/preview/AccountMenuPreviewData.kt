package com.aura.feature.account.presentation.preview

import com.aura.feature.account.domain.model.AccountProfile
import com.aura.feature.account.presentation.menu.AccountMenuUiState
import com.aura.feature.onboarding.domain.model.AuthProvider

object AccountMenuPreviewData {

    val state = AccountMenuUiState(
        profile = AccountProfile(
            handle = "syrex",
            email = "syrex@gmail.com",
            authProvider = AuthProvider.GOOGLE,
        ),
        pushNotifications = true,
    )
}
