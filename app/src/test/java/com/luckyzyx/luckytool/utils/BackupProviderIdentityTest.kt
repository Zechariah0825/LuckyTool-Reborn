package com.luckyzyx.luckytool.utils

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.BuildConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The fork must own its provider authority; it must not compete with the original package. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupProviderIdentityTest {
    @Test fun `provider is private and scoped to installed application id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authority = BuildConfig.APPLICATION_ID + ".FileProvider"
        val provider = context.packageManager.resolveContentProvider(authority, 0)
        assertNotNull(provider)
        assertEquals(authority, provider!!.authority)
        assertFalse(provider.exported)
        assertTrue(provider.grantUriPermissions)
    }

    @Test fun `backup sharing uri uses current package rather than legacy namespace`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "migration-backup-provider.json")
        file.writeText("{}")
        try {
            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".FileProvider", file)
            assertEquals("content", uri.scheme)
            assertEquals(BuildConfig.APPLICATION_ID + ".FileProvider", uri.authority)
        } finally {
            file.delete()
        }
    }
}
