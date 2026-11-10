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

    @Test fun `malformed URI syntax is rejected without throwing`() {
        listOf(
            "https://example.com/%", "https://example.com/%xx",
            "https://[::1", "https://example.com/{bad}",
            "https://example.com/?q=|bad", "https://example.com:65536/",
            "https://example.com\n/path", "https://example.com:abc/",
        ).forEach { assertNull(it, RebornBrowserConfig.validHttpUrl(it)) }
    }

    @Test fun `valid anchors encoded URLs and ports remain usable`() {
        listOf("https://example.com/#section", "https://example.com/a%20b",
            "https://example.com:8443/path", "https://[::1]/").forEach {
            assertEquals(it, RebornBrowserConfig.validHttpUrl(it))
        }
    }

    @Test fun `search placeholders cannot alter destination or fragment`() {
        listOf("https://%s.example.com/", "https://example.com:%s/",
            "https://example.com/#%s", "https://example.com/?q=%s&bad=%zz").forEach {
            assertNull(it, RebornBrowserConfig.validSearchTemplate(it))
        }
        assertEquals("https://example.com/search/%s", RebornBrowserConfig.validSearchTemplate("https://example.com/search/%s"))
    }

    @Test fun `malformed saved entries do not prevent valid entries loading`() {
        val source = "Broken | https://example.com/%xx\nGood | https://example.com/"
        assertEquals(listOf("Good"), RebornBrowserConfig.parseShortcuts(source).map { it.title })
        val engines = "Broken | https://example.com/?q=%s&bad=%xx\nGood | https://example.com/?q=%s"
        assertEquals(listOf("Good"), RebornBrowserConfig.parseCustomSearchEngines(engines).map { it.label })
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

    @Test fun `custom engine IDs distinguish Java hash collisions and retain legacy selections`() {
        val first = "https://example.com/Aa?q=%s"
        val second = "https://example.com/BB?q=%s"
        assertEquals(first.hashCode(), second.hashCode())
        val raw = "First | $first\nSecond | $second"
        val engines = RebornBrowserConfig.parseCustomSearchEngines(raw)
        assertTrue(engines[0].id != engines[1].id)
        assertEquals("Second", RebornBrowserConfig.searchEngineById(engines[1].id, "", "", raw)?.label)
        val oldId = "custom_" + first.hashCode().toUInt().toString(16)
        assertEquals("First", RebornBrowserConfig.searchEngineById(oldId, "", "", raw)?.label)
    }

    @Test fun `settings validation catches bad lines and allows deletion`() {
        assertNull(RebornBrowserConfig.entriesError("", search = true))
        assertNull(RebornBrowserConfig.entriesError("Link | https://example.com/", search = false))
        assertNull(RebornBrowserConfig.entriesError("Engine | https://example.com/?q=%s", search = true))
        assertTrue(RebornBrowserConfig.entriesError("Good | https://example.com/\nBroken", search = false)!!.contains("2"))
        assertTrue(RebornBrowserConfig.entriesError("Engine | https://example.com/", search = true) != null)
        assertTrue(RebornBrowserConfig.entriesError((1..25).joinToString("\n") { "Link $it | https://example.com/$it" }, search = false) != null)
    }

    @Test fun `search query is URL encoded`() {
        val engine = RebornBrowserConfig.searchEngine(RebornBrowserConfig.BING, "", "")!!
        assertEquals("https://www.bing.com/search?q=%E4%BD%A0%E5%A5%BD+world", RebornBrowserConfig.searchUrl(engine, "你好 world"))
        assertTrue(RebornBrowserConfig.searchEngine(RebornBrowserConfig.GOOGLE, "", "")!!.urlTemplate.contains("%s"))
    }
}
