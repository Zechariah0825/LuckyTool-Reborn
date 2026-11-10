package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test

class BrowserDefaultEnginePolicyTest {
    private val google = RebornBrowserConfig.searchEngine("google", "", "")!!
    private val bing = RebornBrowserConfig.searchEngine("bing", "", "")!!

    @Test fun `module request applies once then native selection survives restart`() {
        val token = BrowserDefaultEnginePolicy.requestToken(google, "request1")!!
        assertTrue(BrowserDefaultEnginePolicy.shouldApply(token, null))
        // Browser changed to Bing. Persisted marker still describes the module
        // request, not the native current key, so reopening does not reapply Google.
        val savedMarker = token
        assertFalse(BrowserDefaultEnginePolicy.shouldApply(token, savedMarker))
        assertFalse(BrowserDefaultEnginePolicy.shouldApply(BrowserDefaultEnginePolicy.requestToken(google, "request1"), savedMarker))
    }

    @Test fun `changing module request or selected template reapplies`() {
        val old = BrowserDefaultEnginePolicy.requestToken(google, "request1")
        assertTrue(BrowserDefaultEnginePolicy.shouldApply(BrowserDefaultEnginePolicy.requestToken(bing, "request2"), old))
        assertTrue(BrowserDefaultEnginePolicy.shouldApply(BrowserDefaultEnginePolicy.requestToken(google.copy(urlTemplate = "https://google.com/search?q=%s"), "request1"), old))
    }

    @Test fun `follow browser never writes native choice and same request can be issued later`() {
        val old = BrowserDefaultEnginePolicy.requestToken(google, "request1")
        assertFalse(BrowserDefaultEnginePolicy.shouldApply(null, old))
        assertTrue(BrowserDefaultEnginePolicy.shouldApply(old, null)) // cleared marker
        assertTrue(BrowserDefaultEnginePolicy.shouldApply(BrowserDefaultEnginePolicy.requestToken(google, "request3"), old)) // no browser launch between changes
    }

    @Test fun `renaming engine does not override native choice and marker is bounded`() {
        val token = BrowserDefaultEnginePolicy.requestToken(google, "request1")!!
        assertEquals(token, BrowserDefaultEnginePolicy.requestToken(google.copy(label = "Renamed"), "request1"))
        assertEquals(64, token.length)
        assertFalse(token.contains(google.urlTemplate))
    }
}
