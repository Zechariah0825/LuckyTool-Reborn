package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RebornTabRulesTest {
    @Test fun `browser five-tab navigation retains correct three tabs`() {
        val tabs = listOf("首页", "视频", "搜索+", "小说", "我的")
        assertEquals(listOf("首页", "搜索+", "我的"),
            RebornBrowserConfig.filterFiveBottomTabs(tabs) { true })
        assertNull(RebornBrowserConfig.filterFiveBottomTabs(tabs + "其他") { true })
        assertNull(RebornBrowserConfig.filterFiveBottomTabs(tabs) { it != "小说" })
    }

}
