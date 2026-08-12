package com.aura.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aura.feature.home.presentation.HomeRoute
import com.aura.feature.home.presentation.HomeTab
import com.aura.feature.network.presentation.NetworkRoute
import com.aura.feature.onboarding.domain.model.StartDestination
import com.aura.feature.onboarding.presentation.auth.AuthRoute
import com.aura.feature.onboarding.presentation.bonus.WelcomeBonusRoute
import com.aura.feature.onboarding.presentation.invite.InviteRoute
import com.aura.feature.onboarding.presentation.splash.SplashRoute

enum class AuraStage { SPLASH, AUTH, INVITE, BONUS, HOME }

@Composable
fun AuraRoot(modifier: Modifier = Modifier) {
    var stage by rememberSaveable { mutableStateOf(AuraStage.SPLASH) }

    when (stage) {
        AuraStage.SPLASH -> SplashRoute(
            onFinished = { destination ->
                stage = when (destination) {
                    StartDestination.AUTH -> AuraStage.AUTH
                    StartDestination.INVITE -> AuraStage.INVITE
                    StartDestination.HOME -> AuraStage.HOME
                }
            },
            modifier = modifier,
        )

        AuraStage.AUTH -> AuthRoute(
            onOpenHome = { stage = AuraStage.HOME },
            onOpenInvite = { stage = AuraStage.INVITE },
            modifier = modifier,
        )

        AuraStage.INVITE -> InviteRoute(
            onFinished = { bonusPopupPending ->
                stage = if (bonusPopupPending) AuraStage.BONUS else AuraStage.HOME
            },
            modifier = modifier,
        )

        AuraStage.BONUS -> Box(modifier = modifier) {
            HomeRoute(modifier = Modifier.fillMaxSize())
            WelcomeBonusRoute(onFinished = { stage = AuraStage.HOME })
        }

        AuraStage.HOME -> MainTabs(modifier = modifier)
    }
}

@Composable
private fun MainTabs(modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.HOME) }

    val selectTab: (HomeTab) -> Unit = { selected ->
        if (selected in IMPLEMENTED_TABS) tab = selected
    }

    when (tab) {
        HomeTab.NETWORK -> NetworkRoute(onTabSelected = selectTab, modifier = modifier)
        else -> HomeRoute(onTabSelected = selectTab, modifier = modifier)
    }
}

private val IMPLEMENTED_TABS = setOf(HomeTab.HOME, HomeTab.NETWORK)
