package com.aura.core.push

import javax.inject.Inject
import javax.inject.Singleton

interface PushTokenProvider {
    suspend fun token(): String?
}

@Singleton
class UnavailablePushTokenProvider @Inject constructor() : PushTokenProvider {
    override suspend fun token(): String? = null
}
