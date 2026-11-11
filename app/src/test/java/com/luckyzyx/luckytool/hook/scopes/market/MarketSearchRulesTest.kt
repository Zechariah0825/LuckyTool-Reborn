package com.luckyzyx.luckytool.hook.scopes.market

import org.junit.Assert.*
import org.junit.Test

class MarketSearchRulesTest {
    open class Card(private val code: Int, val title: String = "") {
        fun getCode() = code
    }
    class BrokenCard { fun getCode(): Int = error("schema failure") }
    class WrongCodeType { fun getCode() = "100203" }
    class SearchBar {
        val editable = StringBuilder("synthetic query")
        var selection = 4
        var displayedHint = "suggested word"
        var hintCalls = 0
        var buttonEnabled = true
        fun setSearchHint(hint: String) {
            displayedHint = hint
            hintCalls++
            buttonEnabled = editable.isNotEmpty() || hint.isNotEmpty()
        }
        fun submit() = editable.toString()
    }

    @Test fun `retain everyone-searching card identity and all native history codes`() {
        val cards = listOf(Card(100204, "history alpha"), Card(40117, "history beta"),
            Card(154, "history gamma"), Card(100203, "大家正在搜"))
        val result = MarketSearchRules.filterLanding(cards)!!
        assertEquals(cards.size, result.size)
        cards.forEachIndexed { index, card -> assertSame(card, result[index]) }
    }

    @Test fun `unknown recommendation and standalone heading cards are not retained by title`() {
        val keep = Card(100203, "server renamed title")
        val cards = listOf(Card(100019, "大家正在搜"), Card(4000, "人气热搜"),
            keep, Card(999999, "new promotion"), Card(204, "精品推荐"))
        assertEquals(listOf(keep), MarketSearchRules.filterLanding(cards))
        assertEquals(5, cards.size)
    }

    @Test fun `first page with no target still keeps history but no recommendations`() {
        val history = Card(154, "synthetic history")
        assertEquals(listOf(history), MarketSearchRules.filterLanding(listOf(Card(4000), history)))
    }

    @Test fun `recommendation-only later page becomes an empty batch`() {
        assertTrue(MarketSearchRules.filterLanding(listOf(Card(4000), Card(100019)))!!.isEmpty())
        assertTrue(MarketSearchRules.filterLanding(emptyList<Any>())!!.isEmpty())
    }

    @Test fun `immutable cached input and repeated delivery are not mutated`() {
        val keep = Card(100203)
        val cards = java.util.Collections.unmodifiableList(listOf(Card(4000), keep))
        repeat(3) { assertEquals(listOf(keep), MarketSearchRules.filterLanding(cards)) }
        assertEquals(2, cards.size)
    }

    @Test fun `schema mismatch leaves whole batch for caller to keep unchanged`() {
        for (unsupported in listOf(null, Any(), BrokenCard(), WrongCodeType())) {
            val input = listOf(Card(100203), unsupported, Card(4000))
            assertNull(MarketSearchRules.filterLanding(input))
            assertEquals(3, input.size)
        }
    }

    @Test fun `blanking calls hint API without replacing user text selection or submit`() {
        val bar = SearchBar()
        val editable = bar.editable
        MarketSearchRules.clearHint(bar)
        assertEquals("", bar.displayedHint)
        assertEquals(1, bar.hintCalls)
        assertSame(editable, bar.editable)
        assertEquals(4, bar.selection)
        assertEquals("synthetic query", bar.submit())
        assertTrue(bar.buttonEnabled)
    }

    @Test fun `late repeated hint updates do not erase new input`() {
        val bar = SearchBar()
        MarketSearchRules.clearHint(bar)
        bar.editable.append(" updated")
        bar.selection = bar.editable.length
        repeat(4) { MarketSearchRules.clearHint(bar) }
        assertEquals("synthetic query updated", bar.submit())
        assertEquals(bar.editable.length, bar.selection)
        assertEquals("", bar.displayedHint)
    }

    @Test fun `empty box remains empty and native button state updates`() {
        val bar = SearchBar()
        bar.editable.clear()
        MarketSearchRules.clearHint(bar)
        assertEquals("", bar.submit())
        assertEquals("", bar.displayedHint)
        assertFalse(bar.buttonEnabled)
    }
}
