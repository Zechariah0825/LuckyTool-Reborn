package com.luckyzyx.luckytool.hook.scopes.market

import org.junit.Assert.*
import org.junit.Test

class RebornMarketContentRulesTest {
    class Card(private val title: String?, val apps: List<String> = emptyList()) {
        fun getTitle() = title
    }
    class UnknownCard
    class ThrowingCard { fun getTitle(): String = error("unavailable") }

    private fun filter(cards: List<Any?>, home: Boolean = true) =
        RebornMarketContentRules.filterCards(cards, home, RebornMarketContentRules::cardTitle)

    @Test fun `whole recommendation card disappears while app moments retain identity and order`() {
        val featured = Card("精品随心选", listOf("抖音", "红果免费短剧", "高德地图"))
        val moment = Card("应用时刻")
        val other = Card("热门")
        val original = listOf(featured, moment, other)
        val result = filter(original)!!
        assertEquals(listOf(moment, other), result)
        assertSame(moment, result[0])
        assertEquals(3, original.size)
        assertEquals(3, featured.apps.size)
        assertTrue(result is ArrayList<*>)
    }

    @Test fun `pagination refresh and repeat filtering do not require moment in same batch`() {
        val only = listOf(Card("精品随心选"))
        assertEquals(emptyList<Any>(), filter(only))
        val moment = Card("应用时刻")
        val refreshed = filter(listOf(Card("精品随心选"), moment, Card(" 精品随心选 ")))!!
        assertEquals(listOf(moment), refreshed)
        assertNull(filter(refreshed))
        assertEquals(listOf(moment), filter(listOf(Card("精品随心选"), moment)))
    }

    @Test fun `non-home pages and similar titles remain untouched`() {
        assertNull(filter(listOf(Card("精品随心选"), Card("应用时刻")), home = false))
        assertNull(filter(listOf(Card("精选"), Card("精品随心选推荐"), Card("应用时刻"))))
    }

    @Test fun `null unknown and broken cards are retained and nested app titles are not inspected`() {
        val unknown = UnknownCard()
        val broken = ThrowingCard()
        val nested = Card("应用时刻", listOf("精品随心选"))
        val input = listOf(null, unknown, broken, nested, Card("精品随心选"))
        assertEquals(listOf(null, unknown, broken, nested), filter(input))
        assertNull(filter(emptyList()))
    }

    @Test fun `only verified home fragment classes and subclasses activate the scope`() {
        val prefix = "com.heytap.cdo.client.cards.page.main.home."
        assertTrue(RebornMarketContentRules.isHomeFragment(sequenceOf(prefix + "HomeChildCardFragment")))
        assertTrue(RebornMarketContentRules.isHomeFragment(sequenceOf("custom.Subclass", prefix + "HomeChosenCardFragment")))
        assertFalse(RebornMarketContentRules.isHomeFragment(sequenceOf(prefix + "HomeV3RankCardFragment")))
        assertFalse(RebornMarketContentRules.isHomeFragment(sequenceOf("com.heytap.market.mine.MineFragment")))
        assertFalse(RebornMarketContentRules.isHomeFragment(emptySequence()))
    }
}
