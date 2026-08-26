package com.aura.feature.onboarding

import com.aura.testing.IntegrationTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

abstract class OnboardingTestCase : IntegrationTestCase() {

    internal fun onboarding(
        referrerCode: String? = null,
        body: suspend CoroutineScope.(OnboardingStack) -> Unit,
    ) = runBlocking {
        val stack = OnboardingStack(referrerCode)
        try {
            body(stack)
        } finally {
            stack.close()
        }
    }
}
