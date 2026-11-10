package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test

class BrowserSearchBarRulesTest {
    private val entries = "Duck | https://duckduckgo.com/?q=%s\nExample | https://example.com/search?q=%s"
    private fun engines() = RebornBrowserConfig.availableSearchEngines("", "", entries)

    @Test fun `other bar selections preserve identities alongside fixed default`() {
        val all = engines()
        val selected = setOf("google", all.last().id)
        val actual = RebornBrowserConfig.searchBarEngines(all, selected, "bing")
        assertEquals(listOf("bing", "google", all.last().id), actual.map { it.id })
        assertTrue(RebornBrowserConfig.searchUrl(actual[1], "a & 中").startsWith("https://www.google.com/search?q=a+%26+"))
        assertTrue(RebornBrowserConfig.searchUrl(actual.last(), "a & 中").startsWith("https://example.com/search?q=a+%26+"))
    }

    @Test fun `default is retained first even if its stored switch is off`() {
        val all = engines()
        assertEquals(listOf("google", "bing"), RebornBrowserConfig.searchBarEngines(all, setOf("bing", "google"), "google").map { it.id })
        assertEquals(listOf("google", "bing"), RebornBrowserConfig.searchBarEngines(all, setOf("bing"), "google").map { it.id })
    }

    @Test fun `empty and deleted selections do not reintroduce native tabs`() {
        assertEquals(listOf("bing"), RebornBrowserConfig.searchBarEngines(engines(), emptySet(), "bing").map { it.id })
        assertEquals(listOf("bing"), RebornBrowserConfig.searchBarEngines(engines(), setOf("deleted"), "bing").map { it.id })
        assertTrue(RebornBrowserConfig.searchBarEngines(engines(), setOf("deleted"), "original").isEmpty())
    }

    @Test fun `native default is first and duplicates preserve actual default object`() {
        data class Native(val key: String, val label: String)
        val actualDefault = Native("baidu", "current")
        val items = listOf(Native("bing", "Bing"), Native("baidu", "old"), Native("google", "Google"))
        val selected = RebornBrowserConfig.withSearchBarDefault(items, actualDefault) { it.key }
        assertEquals(listOf("baidu", "bing", "google"), selected.map { it.key })
        assertSame(actualDefault, selected.first())
        assertEquals(listOf(actualDefault), RebornBrowserConfig.withSearchBarDefault(emptyList(), actualDefault) { it.key })
    }

    @Test fun `native default changes do not reorder an existing pager until model refresh`() {
        val bound = listOf("reborn_google", "reborn_bing")
        assertFalse(RebornBrowserConfig.shouldNormalizeSearchPage(bound, bound))
        assertTrue(RebornBrowserConfig.shouldNormalizeSearchPage(bound, null))
        assertTrue(RebornBrowserConfig.shouldNormalizeSearchPage(listOf("reborn_bing", "reborn_google"), bound))
        assertTrue(RebornBrowserConfig.shouldNormalizeSearchPage(listOf("baidu", "360"), bound))
    }

    @Test fun `live browser default wins over an earlier module request`() {
        val all = engines()
        val extras = RebornBrowserConfig.searchBarEngines(all, setOf("google"), "")
        val nativeChoice = all.first { it.id == "bing" }
        val actual = RebornBrowserConfig.withSearchBarDefault(extras, nativeChoice) { it.id }
        assertEquals(listOf("bing", "google"), actual.map { it.id })
        assertSame(nativeChoice, actual.first())
    }

    @Test fun `native choices match existing identity without guessing unknown destinations`() {
        assertEquals("native_baidu", RebornBrowserConfig.nativeSearchBarId("baidu", "Baidu"))
        assertEquals("native_360", RebornBrowserConfig.nativeSearchBarId("360", "360 Search"))
        assertEquals("native_douyin", RebornBrowserConfig.nativeSearchBarId("douyinvideo", "抖音"))
        assertEquals("native_video", RebornBrowserConfig.nativeSearchBarId("server-defined-key", "影视"))
        assertNull(RebornBrowserConfig.nativeSearchBarId("unverified", "Unknown"))
        assertNull(RebornBrowserConfig.nativeSearchBarId("reborn_google", "Google"))
    }

    @Test fun `custom selection survives rename reorder and uses stable preference key`() {
        val original = engines().last()
        val reordered = RebornBrowserConfig.availableSearchEngines("", "", "Renamed | https://example.com/search?q=%s\nDuck | https://duckduckgo.com/?q=%s")
        val selected = RebornBrowserConfig.searchBarEngines(reordered, setOf(original.id), "original").single()
        assertEquals("Renamed", selected.label)
        assertEquals(RebornBrowserConfig.searchBarEngineKey(original.id), RebornBrowserConfig.searchBarEngineKey(selected.id))
        assertNotEquals(RebornBrowserConfig.SEARCH_PROVIDER, RebornBrowserConfig.searchBarEngineKey(selected.id))
    }
}
