package com.luckyzyx.luckytool.hook.scopes.market

import org.junit.Assert.*
import org.junit.Test

class RebornMarketTabRulesTest {
    @Test fun `startup index follows filtered data rather than old index`() {
        val original = listOf("精选", "热门", "应用时刻", "必玩")
        val filtered = RebornMarketTabRules.removeFeatured(original) { it }!!
        assertEquals(listOf("热门", "应用时刻", "必玩"), filtered)
        assertEquals(1, RebornMarketTabRules.appMomentIndex(filtered) { it })
        assertEquals(4, original.size)
    }
    @Test fun `duplicate featured tabs all disappear but other duplicates stay`() {
        val filtered = RebornMarketTabRules.removeFeatured(
            listOf("精选", "应用时刻", "精选", "热门", "热门")) { it }
        assertEquals(listOf("应用时刻", "热门", "热门"), filtered)
    }
    @Test fun `single remaining app moment is a valid startup tab`() {
        val filtered = RebornMarketTabRules.removeFeatured(listOf("精选", "应用时刻")) { it }!!
        assertEquals(0, RebornMarketTabRules.appMomentIndex(filtered) { it })
    }
    @Test fun `unknown lists and missing target remain untouched`() {
        assertNull(RebornMarketTabRules.removeFeatured(listOf("精选", "热门")) { it })
        assertNull(RebornMarketTabRules.removeFeatured(listOf("应用时刻", "热门")) { it })
        assertEquals(-1, RebornMarketTabRules.appMomentIndex(listOf("热门")) { it })
    }
    @Test fun `server label whitespace is normalized`() {
        assertEquals(listOf(" 应用时刻 ", "热门"),
            RebornMarketTabRules.removeFeatured(listOf(" 精选 ", " 应用时刻 ", "热门")) { it })
    }
}
