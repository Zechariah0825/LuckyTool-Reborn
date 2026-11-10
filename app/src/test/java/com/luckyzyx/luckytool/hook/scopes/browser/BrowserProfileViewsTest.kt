package com.luckyzyx.luckytool.hook.scopes.browser

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.heytap.browser.usercenter.my.MyProfileView
import com.platform.sdk.center.widget.CustomSignInAccountInfoView
import com.platform.sdk.center.widget.HeyTapAccountInfoView
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
        assertTrue(BrowserMineRules.isAccountInfo("com.platform.sdk.center.widget.CustomSignInAccountInfoView"))
        assertFalse(BrowserMineRules.isAccountInfo("other.HeyTapAccountInfoView"))
        assertFalse(BrowserMineRules.isAccountInfo("com.platform.sdk.center.widget.GameAccountInfoView"))
    }

    @Test fun `actual inflated subclass resolves both inherited buttons and preserves account card`() {
        val profile = MyProfileView(context)
        val card = LinearLayout(context)
        val info = CustomSignInAccountInfoView(context)
        val avatar = View(context)
        val name = TextView(context).apply { text = "领现金" }
        val vip = TextView(context).apply { text = "VIP" }
        val tools = LinearLayout(context)
        profile.addView(card)
        profile.addView(tools)
        card.addView(info)
        info.addView(avatar)
        info.addView(name)
        info.addView(vip)
        info.imageButton.text = "登录"
        info.plainButton.text = "领现金"

        BrowserProfileViews.cleanProfile(profile)

        assertEquals(View.GONE, info.plainButton.visibility)
        listOf(profile, card, info, avatar, name, vip, tools).forEach {
            assertEquals(View.VISIBLE, it.visibility)
        }
    }

    @Test fun `inherited image button is cleaned again after async restore without hiding login`() {
        val profile = MyProfileView(context)
        val info = CustomSignInAccountInfoView(context)
        profile.addView(info)
        info.plainButton.text = "登录"
        info.imageButton.text = "领现金"
        repeat(2) {
            info.imageButton.visibility = View.VISIBLE
            BrowserProfileViews.cleanAccountInfo(info)
            assertEquals(View.GONE, info.imageButton.visibility)
            assertEquals(View.VISIBLE, info.plainButton.visibility)
        }
        info.imageButton.text = "登录"
        info.imageButton.visibility = View.VISIBLE
        BrowserProfileViews.cleanAccountInfo(info)
        assertEquals(View.VISIBLE, info.imageButton.visibility)
    }

    @Test fun `same SDK control outside the profile is never hidden`() {
        val info = CustomSignInAccountInfoView(context)
        info.plainButton.text = "领现金"
        LinearLayout(context).addView(info)
        BrowserProfileViews.cleanAccountInfo(info)
        assertEquals(View.VISIBLE, info.plainButton.visibility)
    }

    @Test fun `declaring base view remains supported but unrelated subclasses fail closed`() {
        val profile = MyProfileView(context)
        val base = HeyTapAccountInfoView(context)
        val unrelated = object : HeyTapAccountInfoView(context) {}
        base.plainButton.text = "领现金"
        unrelated.plainButton.text = "领现金"
        profile.addView(base)
        profile.addView(unrelated)
        BrowserProfileViews.cleanProfile(profile)
        assertEquals(View.GONE, base.plainButton.visibility)
        assertEquals(View.VISIBLE, unrelated.plainButton.visibility)
    }
}
