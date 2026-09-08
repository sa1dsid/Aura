package com.aura.core.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseCrashReporter(
    private val crashlytics: FirebaseCrashlytics,
) : CrashReporter {

    override fun setUser(id: String?) {
        crashlytics.setUserId(id.orEmpty())
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun record(error: Throwable) {
        crashlytics.recordException(error)
    }
}
