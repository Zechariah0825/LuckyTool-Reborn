package com.luckyzyx.luckytool.hook.scopes.browser

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.heytap.browser.usercenter.my.MyProfileView
import com.platform.sdk.center.sdk.mvvm.model.data.AcInfo
import com.platform.sdk.center.sdk.mvvm.view.ui.CustomSignInVipCard
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
    private data class Tree(val profile: MyProfileView, val card: CustomSignInVipCard, val info: CustomSignInAccountInfoView)
    private fun tree(loggedIn: Boolean = true): Tree {
        val profile = MyProfileView(context)
        val card = CustomSignInVipCard(context).apply { this.loggedIn = loggedIn }
        val info = CustomSignInAccountInfoView(context)
        profile.addView(card); card.addView(info); card.wireNativeButton(info)
        return Tree(profile, card, info)
    }

    @Test fun `logged in promotional alternatives are hidden regardless of caption`() {
        val t = tree()
        for (title in listOf("领现金", "领奖励", "登录", "anything", "")) {
            t.info.imageButton.text = title; t.info.imageButton.visibility = View.VISIBLE
            t.info.plainButton.text = title; t.info.plainButton.visibility = View.VISIBLE
            BrowserProfileViews.cleanProfile(t.profile)
            assertEquals(View.GONE, t.info.imageButton.visibility)
            assertEquals(View.GONE, t.info.plainButton.visibility)
        }
    }

    @Test fun `binding strips remote and third payload without mutating shared account or login flag`() {
        val t = tree(false)
        val button = AcInfo.ButtonInfo("arbitrary server caption")
        val account = AcInfo("user", button)
        val args = arrayOf<Any?>(context, button, false, "fallback")
        t.card.thirdText = "fallback"
        BrowserProfileViews.filterButtonBinding(t.info, args)
        assertNull(args[1]); assertNull(args[3]); assertEquals(false, args[2])
        assertSame(button, account.button); assertEquals("user", account.userName)
        assertNull(t.card.thirdText)
    }

    @Test fun `native unlogged branch remains a clickable login not stale reward action`() {
        val t = tree(false)
        val args = arrayOf<Any?>(context, AcInfo.ButtonInfo("reward"), false, "third")
        BrowserProfileViews.filterButtonBinding(t.info, args)
        t.info.nativeBind(args[1] as? AcInfo.ButtonInfo, args[2] as Boolean, args[3] as? String)
        BrowserProfileViews.cleanAccountInfo(t.info)
        assertEquals(View.VISIBLE, t.info.imageButton.visibility)
        assertEquals(false, t.info.imageButton.tag)
        t.info.imageButton.performClick()
        assertEquals(1, t.card.loginClicks); assertEquals(0, t.card.rewardClicks)
    }

    @Test fun `pre attachment remote binding is repaired to native login on profile attachment`() {
        val t = tree(false)
        t.profile.removeView(t.card)
        t.info.nativeBind(AcInfo.ButtonInfo("new title"), false, "third")
        BrowserProfileViews.cleanAccountInfo(t.info)
        assertEquals(true, t.info.imageButton.tag)
        t.profile.addView(t.card)
        BrowserProfileViews.cleanProfile(t.profile)
        assertEquals(false, t.info.imageButton.tag)
        assertEquals("Native login", t.info.imageButton.text.toString())
        t.info.imageButton.performClick()
        assertEquals(1, t.card.loginClicks); assertEquals(0, t.card.rewardClicks)
    }

    @Test fun `native ordinary login rebind clears prior remote role`() {
        val t = tree()
        t.info.nativeBind(AcInfo.ButtonInfo("reward"), true, null)
        BrowserProfileViews.cleanAccountInfo(t.info)
        assertEquals(View.GONE, t.info.imageButton.visibility)
        t.card.loggedIn = false
        t.info.nativeBind(null, false, null)
        BrowserProfileViews.cleanAccountInfo(t.info)
        assertEquals(View.VISIBLE, t.info.imageButton.visibility)
        t.info.imageButton.performClick()
        assertEquals(1, t.card.loginClicks)
    }

    @Test fun `source refresh and repeated detach reattach do not resurrect rewards`() {
        val t = tree()
        repeat(3) {
            t.profile.removeView(t.card)
            t.card.thirdText = "server-$it"
            t.info.nativeBind(AcInfo.ButtonInfo("different-$it"), true, t.card.thirdText)
            t.profile.addView(t.card)
            BrowserProfileViews.cleanProfile(t.profile)
            assertEquals(View.GONE, t.info.imageButton.visibility)
            assertNull(t.card.thirdText)
        }
    }

    @Test fun `native setters distinguish remote role not text`() {
        val t = tree(false)
        val login = arrayOf<Any?>("any login caption", false)
        BrowserProfileViews.filterSignText(t.info, login)
        assertEquals("any login caption", login[0])
        val remote = arrayOf<Any?>("any promotion caption", true)
        BrowserProfileViews.filterSignText(t.info, remote)
        assertNull(remote[0])
        t.card.loggedIn = true
        val third = arrayOf<Any?>("any third caption", false)
        BrowserProfileViews.filterSignText(t.info, third)
        assertNull(third[0])
    }

    @Test fun `account avatar vip and unrelated login actions are untouched`() {
        val t = tree()
        var clicks = 0
        val survivors = listOf(View(context), TextView(context).apply { text = "领奖励" }, TextView(context).apply { text = "VIP" })
        survivors.forEach { t.info.addView(it); it.setOnClickListener { clicks++ } }
        t.info.nativeBind(AcInfo.ButtonInfo("anything"), true, null)
        BrowserProfileViews.cleanProfile(t.profile)
        (survivors + listOf(t.profile, t.card, t.info)).forEach { assertEquals(View.VISIBLE, it.visibility) }
        survivors.forEach { it.performClick() }; assertEquals(3, clicks)
    }

    @Test fun `same sdk info outside profile and wrong card inside profile are unchanged`() {
        val t = tree()
        t.profile.removeView(t.card)
        t.info.nativeBind(AcInfo.ButtonInfo("reward"), true, null)
        val args = arrayOf<Any?>(context, AcInfo.ButtonInfo("remote"), true, "third")
        BrowserProfileViews.filterButtonBinding(t.info, args)
        BrowserProfileViews.cleanAccountInfo(t.info)
        assertNotNull(args[1]); assertEquals("third", args[3])
        assertEquals(View.VISIBLE, t.info.imageButton.visibility)
        t.card.removeView(t.info); t.profile.addView(t.info)
        BrowserProfileViews.cleanProfile(t.profile)
        assertEquals(View.VISIBLE, t.info.imageButton.visibility)
    }

    @Test fun `known subclass uses inherited private controls and ignores shadow fields`() {
        val t = tree()
        t.card.removeView(t.info)
        val sub = object : CustomSignInAccountInfoView(context) {
            val mImageBtnSignIn = TextView(context).also { addView(it) }
        }
        t.card.addView(sub); t.card.wireNativeButton(sub)
        sub.nativeBind(AcInfo.ButtonInfo("random"), true, null)
        BrowserProfileViews.cleanProfile(t.profile)
        assertEquals(View.GONE, sub.imageButton.visibility)
        assertEquals(View.VISIBLE, sub.mImageBtnSignIn.visibility)
    }

    @Test fun `unknown SDK sibling fails closed`() {
        val t = tree()
        val sibling = object : HeyTapAccountInfoView(context) {}
        t.card.addView(sibling); t.card.wireNativeButton(sibling)
        sibling.nativeBind(AcInfo.ButtonInfo("reward"), true, null)
        BrowserProfileViews.cleanProfile(t.profile)
        assertEquals(View.VISIBLE, sibling.imageButton.visibility)
    }

    @Test fun `already native login does not rebind on every layout`() {
        val t = tree(false)
        t.info.nativeBind(null, false, null)
        val calls = t.info.bindingCalls
        repeat(5) { BrowserProfileViews.cleanProfile(t.profile) }
        assertEquals(calls, t.info.bindingCalls)
        assertEquals(View.VISIBLE, t.info.imageButton.visibility)
    }

    @Test fun `extra known info under the same card is not its owned info`() {
        val t = tree()
        val extra = CustomSignInAccountInfoView(context)
        t.card.addView(extra)
        extra.nativeBind(AcInfo.ButtonInfo("arbitrary promotion"), true, "fallback")
        val args = arrayOf<Any?>(context, AcInfo.ButtonInfo("remote"), true, "third")
        BrowserProfileViews.filterButtonBinding(extra, args)
        BrowserProfileViews.cleanProfile(t.profile)
        assertNotNull(args[1]); assertEquals("third", args[3])
        assertEquals(View.VISIBLE, extra.imageButton.visibility)
        assertFalse(BrowserProfileViews.isRewardInfo(extra))
    }

    @Test fun `detached binding inputs are left intact until scoped attachment`() {
        val t = tree(false)
        t.profile.removeView(t.card)
        val remote = AcInfo.ButtonInfo("anything")
        val args = arrayOf<Any?>(context, remote, false, "fallback")
        BrowserProfileViews.filterButtonBinding(t.info, args)
        assertSame(remote, args[1]); assertEquals("fallback", args[3])
        t.info.nativeBind(args[1] as? AcInfo.ButtonInfo, args[2] as Boolean, args[3] as? String)
        t.profile.addView(t.card)
        BrowserProfileViews.cleanProfile(t.profile)
        assertEquals(false, t.info.imageButton.tag)
        assertEquals(View.VISIBLE, t.info.imageButton.visibility)
        t.info.imageButton.performClick()
        assertEquals(1, t.card.loginClicks); assertEquals(0, t.card.rewardClicks)
    }

}
