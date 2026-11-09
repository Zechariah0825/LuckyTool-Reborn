package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RebornBrowserConfigTest {
    @Test fun `reject unsafe homepage URLs`() {
        assertNull(RebornBrowserConfig.validHttpUrl("javascript:alert(1)"))
        assertNull(RebornBrowserConfig.validHttpUrl("file:///sdcard/secret"))
        assertNull(RebornBrowserConfig.validHttpUrl("https://user:pass@example.com"))
        assertNull(RebornBrowserConfig.validHttpUrl("https://example.com/a b"))
        assertEquals("https://www.google.com", RebornBrowserConfig.validHttpUrl("https://www.google.com"))
    }

    @Test fun `parse edit and delete shortcut lines without invalid entries`() {
        val raw = "Google | https://google.com\nBad | javascript:alert(1)\nBing | https://bing.com\nGoogle copy | https://google.com"
        assertEquals(listOf("Google", "Bing"), RebornBrowserConfig.parseShortcuts(raw).map { it.title })
        assertEquals(listOf("Bing"), RebornBrowserConfig.parseShortcuts("Bing | https://bing.com").map { it.title })
    }

    @Test fun `only valid search URL templates are accepted`() {
        assertNull(RebornBrowserConfig.validSearchTemplate("https://example.com/?q=abc"))
        assertNull(RebornBrowserConfig.validSearchTemplate("https://example.com/?q=%s&more=%s"))
        assertNull(RebornBrowserConfig.validSearchTemplate("javascript:%s"))
        assertEquals("https://example.com/?q=%s", RebornBrowserConfig.validSearchTemplate("https://example.com/?q=%s"))
    }

    @Test fun `multiple custom engines preserve stable ids and valid order`() {
        val source = "DuckDuckGo | https://duckduckgo.com/?q=%s\nInvalid | file:///%s\nBrave | https://search.brave.com/search?q=%s"
        val initial = RebornBrowserConfig.parseCustomSearchEngines(source)
        assertEquals(listOf("DuckDuckGo", "Brave"), initial.map { it.label })
        val reordered = RebornBrowserConfig.parseCustomSearchEngines(source.lines().reversed().joinToString("\n"))
        assertEquals(initial[0].id, reordered.first { it.label == "DuckDuckGo" }.id)
        assertEquals(initial[1], RebornBrowserConfig.searchEngineById(initial[1].id, "", "", source))
    }

    @Test fun `search query is URL encoded`() {
        val engine = RebornBrowserConfig.searchEngine(RebornBrowserConfig.BING, "", "")!!
        assertEquals("https://www.bing.com/search?q=%E4%BD%A0%E5%A5%BD+world", RebornBrowserConfig.searchUrl(engine, "你好 world"))
        assertTrue(RebornBrowserConfig.searchEngine(RebornBrowserConfig.GOOGLE, "", "")!!.urlTemplate.contains("%s"))
    }
}
