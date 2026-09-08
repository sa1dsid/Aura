package com.aura.core.crash

import com.aura.core.common.ApplicationScope
import com.aura.feature.onboarding.data.local.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashUserTracker @Inject constructor(
    private val sessionStore: SessionStore,
    private val crashReporter: CrashReporter,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    fun track() {
        scope.launch {
            sessionStore.account
                .map { it?.id }
                .distinctUntilChanged()
                .collect(crashReporter::setUser)
        }
    }
}
