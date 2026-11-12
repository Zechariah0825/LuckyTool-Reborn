package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test

class BrowserSearchHomeRouteTest {
    private val search = "com.heytap.browser.ai.page.multi.MultiSearchPage"
    @Test fun `explicit Search plus Home retains its native history`() {
        assertTrue(BrowserSearchHomeRoute.shouldPreserve(search, true, true))
    }
    @Test fun `Back background tabs and ordinary webpages are untouched`() {
        assertFalse(BrowserSearchHomeRoute.shouldPreserve(search, true, false))
        assertFalse(BrowserSearchHomeRoute.shouldPreserve(search, false, true))
        assertFalse(BrowserSearchHomeRoute.shouldPreserve("com.heytap.browser.home.NormalHomePageFragment", true, true))
        assertFalse(BrowserSearchHomeRoute.shouldPreserve("com.heytap.browser.webdetails.WebPage", true, true))
        assertFalse(BrowserSearchHomeRoute.shouldPreserve(null, true, true))
    }
}
