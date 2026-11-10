package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

class BrowserSearchIconPolicyTest {
    @Test fun `only site origin is sent with no query or user path`() {
        assertEquals("https://www.google.com", BrowserSearchIconPolicy.origin("https://www.google.com/search?q=%s&token=private"))
        assertEquals("https://example.com", BrowserSearchIconPolicy.origin("http://example.com/private/account/search/%s"))
        assertEquals(listOf("https://example.com/favicon.ico", "https://example.com/apple-touch-icon.png"), BrowserSearchIconPolicy.candidates("https://example.com"))
    }
    @Test fun `credentials private hosts IP literals and nonstandard ports use fallback`() {
        listOf("https://user:password@example.com/?q=%s", "https://localhost/?q=%s", "https://service.local/?q=%s",
            "https://127.0.0.1/?q=%s", "https://[::1]/?q=%s", "https://example.com:8443/?q=%s").forEach {
            assertNull(it, BrowserSearchIconPolicy.origin(it))
        }
    }
    @Test fun `redirects cannot escape origin TLS or introduce credentials and query`() {
        val origin = "https://example.com"
        assertEquals("https://example.com/assets/icon.png", BrowserSearchIconPolicy.redirect(origin, "$origin/favicon.ico", "/assets/icon.png"))
        listOf("http://example.com/icon.png", "https://evil.example/icon.png", "//evil.example/x", "https://user@example.com/icon", "/icon?q=secret", "/icon#fragment").forEach {
            assertNull(it, BrowserSearchIconPolicy.redirect(origin, "$origin/favicon.ico", it))
        }
    }
    @Test fun `DNS private addresses and invalid dimensions are rejected`() {
        listOf("127.0.0.1", "10.0.0.1", "169.254.169.254", "192.168.1.1", "100.64.0.1", "::1", "fd00::1").forEach {
            assertFalse(it, BrowserSearchIconPolicy.publicAddress(InetAddress.getByName(it)))
        }
        assertTrue(BrowserSearchIconPolicy.publicAddress(InetAddress.getByName("8.8.8.8")))
        assertFalse(BrowserSearchIconPolicy.validDimensions(100000, 100000))
        assertFalse(BrowserSearchIconPolicy.validDimensions(0, 64))
    }
    @Test fun `rebind detach and native reuse cannot receive old async icons`() {
        val state = BrowserSearchIconBindings<Any>()
        val view = Any()
        val first = state.bind(view, "https://google.com", "Google", "reborn_google")
        assertTrue(state.mayApply(view, first, true, "reborn_google"))
        assertFalse(state.mayApply(view, first, false, "reborn_google"))
        val second = state.bind(view, "https://bing.com", "Bing", "reborn_bing")
        assertFalse(state.mayApply(view, first, true, "reborn_google"))
        assertFalse(state.mayApply(view, second, true, "baidu"))
        assertTrue(state.mayApply(view, second, true, "reborn_bing"))
        state.remove(view)
        assertFalse(state.mayApply(view, second, true, "reborn_bing"))
    }
}
