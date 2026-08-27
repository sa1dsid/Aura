package com.aura.core.common

private const val DEFAULT_ATTEMPTS = 2

suspend fun <T> runCatchingRetried(
    attempts: Int = DEFAULT_ATTEMPTS,
    block: suspend () -> T,
): Result<T> {
    var failure: Throwable? = null

    repeat(attempts) {
        val result = runCatchingCancellable { block() }
        result.onSuccess { return result }
        failure = result.exceptionOrNull()
    }

    return Result.failure(failure ?: IllegalStateException("no attempts"))
}
