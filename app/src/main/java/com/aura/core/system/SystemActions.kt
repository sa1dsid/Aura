package com.aura.core.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun Context.openUrl(url: String): Boolean {
    if (url.isBlank()) return false
    return startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

fun Context.openVpnSettings() {
    val opened = startSafely(Intent(Settings.ACTION_VPN_SETTINGS))
    if (!opened) startSafely(Intent(Settings.ACTION_WIRELESS_SETTINGS))
}

fun Context.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startSafely(Intent.createChooser(intent, null))
}

private fun Context.startSafely(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (notFound: ActivityNotFoundException) {
    false
}
