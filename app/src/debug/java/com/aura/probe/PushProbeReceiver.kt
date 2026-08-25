package com.aura.probe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aura.core.push.PushNotifier

class PushProbeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        PushNotifier(context.applicationContext).show(
            title = intent.getStringExtra(EXTRA_TITLE) ?: DEFAULT_TITLE,
            body = intent.getStringExtra(EXTRA_BODY) ?: DEFAULT_BODY,
            link = intent.getStringExtra(EXTRA_LINK),
        )
    }

    private companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_LINK = "link"
        const val DEFAULT_TITLE = "Aura"
        const val DEFAULT_BODY = "Probe notification"
    }
}
