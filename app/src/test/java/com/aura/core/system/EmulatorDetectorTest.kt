package com.aura.core.system

import com.aura.BuildConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class EmulatorDetectorTest {

    @Test
    fun `the debug build never calls a device an emulator`() {
        describeDevice(fingerprint = "generic/sdk_gphone64_x86_64", model = "sdk_gphone64")

        assertTrue(BuildConfig.ALLOW_EMULATOR)
        assertFalse(EmulatorDetector().isEmulator)
    }

    @Test
    fun `the marks that give an emulator away`() {
        val giveaways = listOf(
            "fingerprint" to "generic/sdk_gphone64_x86_64/emu64x",
            "fingerprint" to "unknown/unknown/unknown",
            "fingerprint" to "Android/Emulator/build",
            "model" to "google_sdk",
            "model" to "Android SDK built for x86",
            "model" to "Emulator X",
            "product" to "sdk_gphone64_x86_64",
            "product" to "emulator_arm64",
            "hardware" to "goldfish",
            "hardware" to "ranchu",
            "hardware" to "vbox86",
        )

        for ((field, value) in giveaways) {
            describeDevice()
            ReflectionHelpers.setStaticField(
                android.os.Build::class.java,
                field.uppercase(),
                value,
            )

            assertTrue("$field=$value", detects())
        }
    }

    @Test
    fun `a generic brand alone is not enough without a generic device`() {
        describeDevice(brand = "generic", device = "pixel")

        assertFalse(detects())
    }

    @Test
    fun `a generic brand on a generic device gives it away`() {
        describeDevice(brand = "generic_x86", device = "generic_x86")

        assertTrue(detects())
    }

    @Test
    fun `an ordinary phone is left alone`() {
        describeDevice(
            fingerprint = "google/husky/husky:15/AP4A.250105.002/release-keys",
            model = "Pixel 8 Pro",
            product = "husky",
            hardware = "husky",
            brand = "google",
            device = "husky",
        )

        assertFalse(detects())
    }

    private fun detects(): Boolean =
        ReflectionHelpers.callInstanceMethod<Boolean>(EmulatorDetector(), "detect")

    private fun describeDevice(
        fingerprint: String = "google/husky/husky:15/AP4A.250105.002/release-keys",
        model: String = "Pixel 8 Pro",
        product: String = "husky",
        hardware: String = "husky",
        brand: String = "google",
        device: String = "husky",
    ) {
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "FINGERPRINT", fingerprint)
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "MODEL", model)
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "PRODUCT", product)
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "HARDWARE", hardware)
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "BRAND", brand)
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "DEVICE", device)
    }
}
