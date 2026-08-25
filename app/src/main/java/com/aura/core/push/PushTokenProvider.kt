package com.aura.core.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface PushTokenProvider {
    suspend fun token(): String?
}

@Singleton
class FirebasePushTokenProvider @Inject constructor(
    private val messaging: FirebaseMessaging,
) : PushTokenProvider {

    @Suppress("DEPRECATION")
    override suspend fun token(): String? = suspendCancellableCoroutine { continuation ->
        messaging.token.addOnCompleteListener { task ->
            val token = if (task.isSuccessful) task.result else null
            continuation.resume(token?.takeIf(String::isNotBlank))
        }
    }
}
