package com.luckyzyx.luckytool.hook.scopes.browser

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = android.app.Application::class)
class BrowserProfileViewsTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun `both sign controls are checked not just the first existing id`() {
        val info = LinearLayout(context)
        val login = TextView(context).apply { text = "登录"; visibility = View.GONE }
        val cash = TextView(context).apply { text = "领现金" }
        info.addView(login); info.addView(cash)
        BrowserProfileViews.hideCashButtons(info, listOf(login, cash), "领现金")
        assertEquals(View.GONE, cash.visibility)
        assertEquals(View.VISIBLE, info.visibility)
    }

    @Test fun `login avatar name and unrelated rewards remain visible`() {
        val info = LinearLayout(context)
        val login = TextView(context).apply { text = "登录" }
        val username = TextView(context).apply { text = "领现金" }
        val outside = TextView(context).apply { text = "领现金" }
        info.addView(login); info.addView(username)
        BrowserProfileViews.hideCashButtons(info, listOf(login, outside), "领现金")
        assertEquals(View.VISIBLE, login.visibility)
        assertEquals(View.VISIBLE, username.visibility)
        assertEquals(View.VISIBLE, outside.visibility)
    }

    @Test fun `refresh can restore login after cash has been hidden`() {
        val info = LinearLayout(context)
        val button = TextView(context).apply { text = "领现金" }
        info.addView(button)
        repeat(2) {
            button.visibility = View.VISIBLE
            BrowserProfileViews.hideCashButtons(info, listOf(button), "领现金")
            assertEquals(View.GONE, button.visibility)
        }
        button.text = "登录"
        button.visibility = View.VISIBLE
        BrowserProfileViews.hideCashButtons(info, listOf(button), "领现金")
        assertEquals(View.VISIBLE, button.visibility)
    }

    @Test fun `native account selector is exact`() {
        assertTrue(BrowserMineRules.isAccountInfo("com.platform.sdk.center.widget.HeyTapAccountInfoView"))
        assertFalse(BrowserMineRules.isAccountInfo("other.HeyTapAccountInfoView"))
    }
}
