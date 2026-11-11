package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test

@Suppress("UNUSED_PARAMETER")
class BrowserHomeBackRoutesTest {
    private class Page
    private class Params
    private class Tab {
        fun x9(params: Params, animated: Boolean) = Unit
        fun P(params: Params, animated: Boolean) = Unit
        fun C8(page: Page, animated: Boolean) = Unit
        fun M(animated: Boolean, hard: Boolean): Boolean = false
    }
    private class ReferenceTab {
        fun s9(params: Params, animated: Boolean) = Unit
        fun Q(params: Params, animated: Boolean) = Unit
        fun A8(page: Page, animated: Boolean) = Unit
        fun N(animated: Boolean, hard: Boolean): Boolean = false
    }
    private class Navigation {
        fun E(): Int = 0
        fun B(page: Page): Int = 0
    }
    private class WrongNavigation {
        fun E(): Boolean = false
        fun B(page: Page): Int = 0
    }
    private class WrongTab {
        fun x9(params: Params, animated: Boolean) = Unit
        fun P(params: Params, animated: Boolean) = Unit
        fun C8(page: Page, animated: Boolean) = Unit
        fun M(animated: Boolean, hard: Boolean) = Unit
    }

    @Test fun `only actual current version selects freshly verified Back routes`() {
        val routes = BrowserHomeBackRoutes.select("40.10.21.9.6", 10210906L, "xh")!!
        assertEquals("M", routes.backwardPage)
        assertEquals("x9", routes.permissionLoad)
        assertEquals("P", routes.dispatchLoad)
        assertEquals("C8", routes.showPage)
        assertTrue(routes.matches(Tab::class.java, Page::class.java, Params::class.java, Navigation::class.java))
    }

    @Test fun `reference has independently verified showPage A8 and Back N`() {
        val routes = BrowserHomeBackRoutes.select("40.10.25.2", 10250200L, "yh")!!
        assertEquals("A8", routes.showPage)
        assertEquals("N", routes.backwardPage)
        assertTrue(routes.matches(ReferenceTab::class.java, Page::class.java, Params::class.java, Navigation::class.java))
        assertFalse(routes.matches(Tab::class.java, Page::class.java, Params::class.java, Navigation::class.java))
    }

    @Test fun `unknown and mixed versions cannot inherit current obfuscation`() {
        assertNull(BrowserHomeBackRoutes.select("40.10.21.9.6", 10210906L, "yh"))
        assertNull(BrowserHomeBackRoutes.select("40.10.21.9.6", 10210907L, "xh"))
        assertNull(BrowserHomeBackRoutes.select("40.10.21.9.7", 10210906L, "xh"))
    }

    @Test fun `wrong result type or parameter shape rejects installation`() {
        val routes = BrowserHomeBackRoutes.select("40.10.21.9.6", 10210906L, "xh")!!
        assertFalse(routes.matches(WrongTab::class.java, Page::class.java, Params::class.java, Navigation::class.java))
        assertFalse(routes.matches(Tab::class.java, Page::class.java, Params::class.java, WrongNavigation::class.java))
        assertFalse(routes.matches(Tab::class.java, Page::class.java, Any::class.java, Navigation::class.java))
    }
}
