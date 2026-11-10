package com.luckyzyx.luckytool.ui.theme

import android.app.Application
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DynamicColorCompatibilityTest {
    @Test
    @Config(sdk = [30])
    fun `Android 11 uses a neutral seed instead of unavailable dynamic resources`() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(Color(0xFF808080), systemAccentSeed(context, false))
        assertEquals(Color(0xFF808080), systemAccentSeed(context, true))
    }

    @Test
    @Config(sdk = [32])
    fun `supported platforms retain their actual dynamic seed`() {
        val context = RuntimeEnvironment.getApplication()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertEquals(dynamicLightColorScheme(context).primary, systemAccentSeed(context, false))
            assertEquals(dynamicDarkColorScheme(context).primary, systemAccentSeed(context, true))
        } else {
            error("This test requires the configured Android 12 SDK")
        }
    }
}
