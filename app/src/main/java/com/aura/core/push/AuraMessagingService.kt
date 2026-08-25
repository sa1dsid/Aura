package com.aura.core.push

import com.aura.core.common.ApplicationScope
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuraMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notifier: PushNotifier

    @Inject
    lateinit var tokenRepository: PushTokenRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        applicationScope.launch { tokenRepository.register() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = message.notification
        notifier.show(
            title = payload?.title ?: message.data[DATA_TITLE],
            body = payload?.body ?: message.data[DATA_BODY],
            link = message.data[DATA_LINK],
        )
    }

    private companion object {
        const val DATA_TITLE = "title"
        const val DATA_BODY = "body"
        const val DATA_LINK = "link"
    }
}
