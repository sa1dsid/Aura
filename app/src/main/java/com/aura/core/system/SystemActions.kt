package com.aura.core.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

fun Context.openUrl(url: String): Boolean {
    if (url.isBlank()) return false
    return startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

fun Context.openSocialLink(appUrl: String?, webUrl: String) {
    if (appUrl != null && openUrl(appUrl)) return
    openUrl(webUrl)
}

fun Context.openVpnSettings() {
    val opened = startSafely(Intent(Settings.ACTION_VPN_SETTINGS))
    if (!opened) startSafely(Intent(Settings.ACTION_WIRELESS_SETTINGS))
}

fun Context.isBatteryOptimizationIgnored(): Boolean {
    val power = getSystemService(PowerManager::class.java) ?: return false
    return power.isIgnoringBatteryOptimizations(packageName)
}

fun Context.requestIgnoreBatteryOptimization(): Boolean {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.parse("package:$packageName"))

    if (startSafely(direct)) return true

    return startSafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
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
