package com.aura.core.crash

object NoCrashReporter : CrashReporter {

    override fun setUser(id: String?) = Unit

    override fun log(message: String) = Unit

    override fun record(error: Throwable) = Unit
}
