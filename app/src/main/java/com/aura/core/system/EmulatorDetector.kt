package com.aura.core.system

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmulatorDetector @Inject constructor() {

    val isEmulator: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) { detect() }

    private fun detect(): Boolean {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val device = Build.DEVICE.orEmpty()

        return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            fingerprint.contains("emulator", ignoreCase = true) ||
            model.contains("google_sdk", ignoreCase = true) ||
            model.contains("Emulator", ignoreCase = true) ||
            model.contains("Android SDK built for", ignoreCase = true) ||
            product.contains("sdk", ignoreCase = true) ||
            product.contains("emulator", ignoreCase = true) ||
            hardware.contains("goldfish", ignoreCase = true) ||
            hardware.contains("ranchu", ignoreCase = true) ||
            hardware.contains("vbox", ignoreCase = true) ||
            (brand.startsWith("generic") && device.startsWith("generic"))
    }
}
