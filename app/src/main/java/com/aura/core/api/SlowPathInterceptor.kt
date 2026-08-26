package com.aura.core.api

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val AUTH_PREFIX = "/api/v1/auth/"

private const val SLOW_READ_TIMEOUT_SECONDS = 30

@Singleton
class SlowPathInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.encodedPath.startsWith(AUTH_PREFIX)) return chain.proceed(request)

        return chain
            .withReadTimeout(SLOW_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .proceed(request)
    }
}
