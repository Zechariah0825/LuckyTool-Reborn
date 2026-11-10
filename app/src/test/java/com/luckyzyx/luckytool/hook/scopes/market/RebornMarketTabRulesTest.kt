package com.luckyzyx.luckytool.hook.scopes.market

import org.junit.Assert.*
import org.junit.Test

class RebornMarketTabRulesTest {
    private data class Page(val title: String, val kind: MarketPageKind, val payload: String)

    // Captured native order: remove firstPagePos from factory inputs, create each
    // remaining route, then insert HomeChosen under the removed title at that index.
    private fun nativeFactory(tabs: List<String>, firstPagePos: Int): List<Page> {
        val remaining = tabs.toMutableList().apply { removeAt(firstPagePos) }
        val pages = remaining.map {
            Page(it, if (it == "应用时刻") MarketPageKind.APP_MOMENT else MarketPageKind.OTHER, "route:$it")
        }.toMutableList()
        pages.add(firstPagePos, Page(tabs[firstPagePos], MarketPageKind.CHOSEN, "精品随心选 + 『附近地区』的人都在下"))
        return pages
    }

    private fun filter(tabs: List<String>, pages: List<Page>, selected: String? = null) =
        RebornMarketTabRules.filterPairs(tabs, pages, pages.map { MarketPageSignature(it.title, it.kind) }, selected) { it }

    @Test fun `native factory sees original DTO then featured title and real feed disappear together`() {
        val tabs = listOf("精选", "应用时刻", "热门", "必备")
        val pages = nativeFactory(tabs, 0)
        val result = filter(tabs, pages)!!
        assertEquals(listOf("应用时刻", "热门", "必备"), result.tabs)
        assertSame(pages[1], result.pages[0])
        assertEquals("route:应用时刻", result.pages[0].payload)
        assertFalse(result.pages.any { it.kind == MarketPageKind.CHOSEN })
        assertEquals(0, result.selectedIndex)
        assertEquals(4, tabs.size)
        assertEquals(4, pages.size)
    }

    @Test fun `old early filtering and first position rewrite is detected as wrong page mapping`() {
        val alteredDto = listOf("应用时刻", "热门", "必备")
        val wrongPages = nativeFactory(alteredDto, 0)
        assertEquals(MarketPageKind.CHOSEN, wrongPages[0].kind)
        assertNull(filter(alteredDto, wrongPages))
        val titles = listOf("精选", "应用时刻", "热门")
        assertNull(filter(titles, nativeFactory(titles, 1)))
    }

    @Test fun `original featured position may be nonzero without replacing native moment`() {
        val tabs = listOf("热门", "精选", "必备", "应用时刻")
        val pages = nativeFactory(tabs, 1)
        val result = filter(tabs, pages)!!
        assertEquals(listOf("热门", "必备", "应用时刻"), result.tabs)
        assertEquals(2, result.selectedIndex)
        assertSame(pages[3], result.pages[2])
    }

    @Test fun `refresh and repeated binding keep user selected tab with matching page`() {
        val tabs = listOf("精选", "应用时刻", "热门", "必备")
        repeat(3) {
            val result = filter(tabs, nativeFactory(tabs, 0), "必备")!!
            assertEquals(2, result.selectedIndex)
            assertEquals(0, result.homeIndex)
            assertEquals("必备", result.pages[result.selectedIndex].title)
            assertNull(filter(result.tabs, result.pages, "必备"))
        }
    }

    @Test fun `removed missing or initial selection falls back to native moment`() {
        val tabs = listOf("精选", "热门", "应用时刻")
        listOf(null, "精选", "已删除的栏目").forEach { selected ->
            assertEquals(1, filter(tabs, nativeFactory(tabs, 0), selected)!!.selectedIndex)
        }
    }

    @Test fun `unknown types mismatched pairs and partial lists fail open`() {
        val tabs = listOf("精选", "应用时刻", "热门")
        val pages = nativeFactory(tabs, 0)
        assertNull(filter(tabs, pages.dropLast(1)))
        assertNull(filter(tabs, pages.reversed()))
        assertNull(filter(tabs, pages.map { it.copy(kind = MarketPageKind.OTHER) }))
        assertNull(filter(emptyList(), emptyList()))
        assertNull(filter(listOf("精选"), nativeFactory(listOf("精选"), 0)))
        assertNull(filter(listOf("精选", "应用时刻", "应用时刻"), nativeFactory(listOf("精选", "应用时刻", "应用时刻"), 0)))
    }

    @Test fun `spaces normalize but content remains identical and substring matches are forbidden`() {
        val tabs = listOf(" 精选 ", " 应用时刻 ")
        val pages = listOf(Page("精选", MarketPageKind.CHOSEN, "recommendations"), Page("应用时刻", MarketPageKind.APP_MOMENT, "original"))
        assertSame(pages[1], filter(tabs, pages)!!.pages.single())
        assertNull(filter(listOf("精选推荐", "应用时刻"), pages))
    }

    @Test fun `fragment identity must be actual native moment not generic home child or label`() {
        assertEquals(MarketPageKind.APP_MOMENT, RebornMarketTabRules.pageKind(sequenceOf(
            "custom.Subclass", "com.heytap.cdo.client.cn.appmoment.AppMomentCardsFragment")))
        assertEquals(MarketPageKind.CHOSEN, RebornMarketTabRules.pageKind(sequenceOf(
            "com.heytap.cdo.client.cards.page.main.home.HomeChosenCardFragment")))
        assertEquals(MarketPageKind.OTHER, RebornMarketTabRules.pageKind(sequenceOf(
            "com.heytap.cdo.client.cards.page.main.home.HomeChildCardFragment")))
        assertEquals(MarketPageKind.OTHER, RebornMarketTabRules.pageKind(emptySequence()))
    }
    @Test fun `native generic child fallback requires exact app moment route`() {
        val child = "com.heytap.cdo.client.cards.page.main.home.HomeChildCardFragment"
        assertEquals(MarketPageKind.APP_MOMENT, RebornMarketTabRules.pageKind(sequenceOf(child), "oap://mk/appmoment/main?scene=home"))
        listOf(null, "", "oap://mk/appmoment/main/other", "oap://mk/nearby", "https://mk/appmoment/main", "oap://other/appmoment/main", "invalid URI").forEach {
            assertEquals(MarketPageKind.OTHER, RebornMarketTabRules.pageKind(sequenceOf(child), it))
        }
        assertEquals(MarketPageKind.OTHER, RebornMarketTabRules.pageKind(sequenceOf("unknown.Fragment"), "oap://mk/appmoment/main"))
        assertEquals(MarketPageKind.CHOSEN, RebornMarketTabRules.pageKind(sequenceOf(
            "com.heytap.cdo.client.cards.page.main.home.HomeChosenCardFragment"), "oap://mk/appmoment/main"))
    }

}
