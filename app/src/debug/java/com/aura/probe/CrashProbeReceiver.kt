package com.aura.probe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashProbeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_CRASH

        if (mode == MODE_OFF) {
            crashlytics.isCrashlyticsCollectionEnabled = false
            return
        }

        crashlytics.isCrashlyticsCollectionEnabled = true
        val error = ProbeCrash(intent.getStringExtra(EXTRA_MESSAGE) ?: DEFAULT_MESSAGE)

        when (mode) {
            MODE_ON -> Unit
            MODE_NON_FATAL -> crashlytics.recordException(error)
            else -> throw error
        }
    }

    private companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_MESSAGE = "message"
        const val MODE_ON = "on"
        const val MODE_OFF = "off"
        const val MODE_NON_FATAL = "nonfatal"
        const val MODE_CRASH = "crash"
        const val DEFAULT_MESSAGE = "Probe crash"
    }
}

private class ProbeCrash(message: String) : RuntimeException(message)
