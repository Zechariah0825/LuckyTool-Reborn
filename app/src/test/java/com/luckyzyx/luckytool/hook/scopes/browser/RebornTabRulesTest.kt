package com.luckyzyx.luckytool.hook.scopes.browser

import com.luckyzyx.luckytool.hook.scopes.market.RebornMarketTabRules
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

    @Test fun `market only removes featured when app moment exists`() {
        val tabs = listOf("应用时刻", "精选", "热门", "必备", "必玩", "软件榜")
        assertEquals(listOf("应用时刻", "热门", "必备", "必玩", "软件榜"),
            RebornMarketTabRules.removeFeatured(tabs) { it })
        assertEquals(6, tabs.size)
        assertNull(RebornMarketTabRules.removeFeatured(listOf("首页", "精选", "排行")) { it })
    }
}
