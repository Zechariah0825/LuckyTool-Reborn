package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test

class BrowserMineRulesTest {
    @Test fun `only the profile bookshelf card is targeted not shared containers`() {
        assertTrue(BrowserMineRules.isBookshelf("com.heytap.browser.usercenter.my.bookshelf.MyBookshelfHistoryCard"))
        assertFalse(BrowserMineRules.isBookshelf("com.heytap.browser.usercenter.my.game.MyGameCard"))
        assertFalse(BrowserMineRules.isBookshelf("android.widget.LinearLayout"))
        assertFalse(BrowserMineRules.isBookshelf("MyBookshelfHistoryCard"))
    }

    @Test fun `profile and account selectors require complete known class names`() {
        assertTrue(BrowserMineRules.isProfile("com.heytap.browser.usercenter.my.MyProfileView"))
        assertFalse(BrowserMineRules.isProfile("other.MyProfileView"))
        assertTrue(BrowserMineRules.isAccountCard("com.platform.sdk.center.sdk.mvvm.view.ui.CustomSignInVipCard"))
        assertFalse(BrowserMineRules.isAccountCard("android.widget.TextView"))
    }

}
