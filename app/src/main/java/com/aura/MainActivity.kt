package com.aura

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.aura.core.designsystem.theme.AuraTheme
import com.aura.feature.onboarding.domain.repository.InviteRepository
import com.aura.navigation.AuraRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var inviteRepository: InviteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        consumeInviteLink(intent)
        setContent {
            AuraTheme {
                AuraRoot(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeInviteLink(intent)
    }

    private fun consumeInviteLink(intent: Intent?) {
        val code = intent?.data?.inviteCode() ?: return
        lifecycleScope.launch { inviteRepository.rememberDeepLinkCode(code) }
    }

    private fun Uri.inviteCode(): String? =
        getQueryParameter("code") ?: pathSegments.lastOrNull()?.takeIf { it.isNotBlank() }

    private companion object {
        const val TRANSPARENT = 0x00000000
    }
}
