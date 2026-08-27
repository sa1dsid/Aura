package com.aura.testing

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

class CountingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {

    var dispatches = 0
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatches++
        delegate.dispatch(context, block)
    }
}
