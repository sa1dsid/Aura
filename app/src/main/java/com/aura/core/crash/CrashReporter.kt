package com.aura.core.crash

interface CrashReporter {

    fun setUser(id: String?)

    fun log(message: String)

    fun record(error: Throwable)
}
