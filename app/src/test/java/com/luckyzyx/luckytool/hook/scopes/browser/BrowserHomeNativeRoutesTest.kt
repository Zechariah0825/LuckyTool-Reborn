package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test

@Suppress("UNUSED_PARAMETER")
class BrowserHomeNativeRoutesTest {
    private class Page
    private class Current {
        fun g1(animated: Boolean) = Unit
        fun g0(last: Page, current: Page) = Unit
        fun h0() = Unit // Real current APK has this unrelated zero-argument method.
    }
    private class Reference {
        fun h1(animated: Boolean) = Unit
        fun h0(last: Page, current: Page) = Unit
    }
    private class WrongReturn {
        fun g1(animated: Boolean): Boolean = true
        fun g0(last: Page, current: Page) = Unit
    }
    private class Partial {
        fun g1(animated: Boolean) = Unit
        fun h0(last: Page, current: Page) = Unit
    }

    @Test fun `current exact version selects g routes and ignores unrelated h0`() {
        val routes = BrowserHomeNativeRoutes.select("40.10.21.9.6", 10210906, "xh")!!
        assertEquals("g1", routes.returnHome)
        assertEquals("g0", routes.pageChanged)
        assertTrue(routes.matches(Current::class.java, Page::class.java))
    }

    @Test fun `reference exact version selects h routes`() {
        val routes = BrowserHomeNativeRoutes.select("40.10.25.2", 10250200, "yh")!!
        assertEquals("h1", routes.returnHome)
        assertEquals("h0", routes.pageChanged)
        assertTrue(routes.matches(Reference::class.java, Page::class.java))
    }

    @Test fun `unknown version is rejected even if an identically shaped class exists`() {
        assertNull(BrowserHomeNativeRoutes.select("40.10.26.0", 10260000, "yh"))
        assertNull(BrowserHomeNativeRoutes.select(null, 10210906, "xh"))
        assertNull(BrowserHomeNativeRoutes.select("40.10.21.9.6_fake", 10210906, "xh"))
    }

    @Test fun `mismatched version code or creation signature cannot choose a mapping`() {
        assertNull(BrowserHomeNativeRoutes.select("40.10.21.9.6", 10250200, "xh"))
        assertNull(BrowserHomeNativeRoutes.select("40.10.25.2", 10250200, "xh"))
        assertNull(BrowserHomeNativeRoutes.select("40.10.21.9.6", 10210906, "yh"))
    }

    @Test fun `wrong class pair cannot fall through to another versions methods`() {
        val routes = BrowserHomeNativeRoutes.select("40.10.21.9.6", 10210906, "xh")!!
        assertFalse(routes.matches(Reference::class.java, Page::class.java))
        assertFalse(routes.matches(Partial::class.java, Page::class.java))
    }

    @Test fun `non void return and wrong page parameter types fail closed`() {
        val routes = BrowserHomeNativeRoutes.select("40.10.21.9.6", 10210906, "xh")!!
        assertFalse(routes.matches(WrongReturn::class.java, Page::class.java))
        assertFalse(routes.matches(Current::class.java, Any::class.java))
    }
}
