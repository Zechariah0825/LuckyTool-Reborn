package com.luckyzyx.luckytool.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import com.luckyzyx.luckytool.R
import java.util.Locale
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.ui.activity.MainActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForkIdentityTest {
    @Test fun `installed identity is independent while implementation namespace remains compatible`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("io.github.zechariah0825.luckytoolreborn", BuildConfig.APPLICATION_ID)
        assertEquals(BuildConfig.APPLICATION_ID, context.packageName)
        assertEquals("com.luckyzyx.luckytool.ui.activity.MainActivity", MainActivity::class.java.name)
        assertEquals(BuildConfig.APPLICATION_ID, RebornReleasePolicy.APPLICATION_ID)
    }

    @Test fun `desktop alias belongs to new package and targets existing activity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alias = ComponentName(context.packageName, context.packageName + ".Hide")
        val activity = context.packageManager.getActivityInfo(alias, 0)
        assertEquals(MainActivity::class.java.name, activity.targetActivity)
        assertTrue(activity.enabled)
        assertTrue(activity.exported)
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        assertTrue(context.packageManager.queryIntentActivities(intent, 0).any { it.activityInfo.name == alias.className })
    }

    @Test fun `display name stays Reborn in all existing name overrides`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (language in listOf("en", "zh-CN", "vi-VN", "tr-TR")) {
            val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
            assertEquals("LuckyTool Reborn", context.createConfigurationContext(config).getString(R.string.app_name))
        }
    }

    @Test fun `new application does not claim upstream file provider`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNull(context.packageManager.resolveContentProvider("com.luckyzyx.luckytool.FileProvider", 0))
    }
}
