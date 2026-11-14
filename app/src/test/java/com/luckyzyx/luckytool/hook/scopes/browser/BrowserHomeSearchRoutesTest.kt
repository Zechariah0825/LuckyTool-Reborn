package com.luckyzyx.luckytool.hook.scopes.browser

import org.junit.Assert.*
import org.junit.Test

class BrowserHomeSearchRoutesTest {
    class Home {
        fun u2(focus: Boolean): Boolean = focus
        fun y1(): Boolean = true
        fun U1(): Boolean = true
        fun onBackPressed(): Boolean = false
    }
    class Direct { fun getType(): Int = 7 }
    class WrongDirect { fun getType(): Boolean = true }
    class WrongHome {
        fun u2(focus: Boolean) { }
        fun y1(): Boolean = true
        fun U1(): Boolean = true
        fun onBackPressed(): Boolean = false
    }
    @Test fun `reference mapping is exact and not chosen from similarly named candidates`() {
        val route = BrowserHomeSearchRoutes.select("40.10.25.2", 10250200, "yh")!!
        assertEquals("u2", route.selectSearch); assertEquals("y1", route.isSearch)
        assertEquals("U1", route.backToMajor); assertEquals("lv1.c", route.directPageClass)
        assertTrue(route.copy(directPageClass = Direct::class.java.name).matches(Home::class.java, Direct::class.java))
    }
    @Test fun `unknown same shape version code and creation cannot authorize mapping`() {
        assertNull(BrowserHomeSearchRoutes.select("40.10.25.3", 10250200, "yh"))
        assertNull(BrowserHomeSearchRoutes.select("40.10.25.2", 10250201, "yh"))
        assertNull(BrowserHomeSearchRoutes.select("40.10.25.2", 10250200, "xh"))
        assertNull(BrowserHomeSearchRoutes.select(null, 10250200, "yh"))
    }
    @Test fun `return type and actual selected child class must match`() {
        val route = BrowserHomeSearchRoutes.select("40.10.25.2", 10250200, "yh")!!
        assertFalse(route.matches(Home::class.java, Direct::class.java))
        val fixture = route.copy(directPageClass = Direct::class.java.name)
        assertFalse(fixture.matches(WrongHome::class.java, Direct::class.java))
        assertFalse(route.copy(directPageClass = WrongDirect::class.java.name).matches(Home::class.java, WrongDirect::class.java))
    }
    class CurrentHome {
        fun t2(focus: Boolean): Boolean = focus
        fun x1(): Boolean = true
        fun U1(): Boolean = true
        fun onBackPressed(): Boolean = false
        // Same-shape y1 exists in the APK but means availability, not selection.
        fun y1(): Boolean = false
    }
    @Test fun `current native Dock chain selects t2 x1 and uq1 c not reference candidates`() {
        val route = BrowserHomeSearchRoutes.select("40.10.21.9.6", 10210906, "xh")!!
        assertEquals("t2", route.selectSearch); assertEquals("x1", route.isSearch)
        assertEquals("U1", route.backToMajor); assertEquals("uq1.c", route.directPageClass)
        val fixture = route.copy(directPageClass = Direct::class.java.name)
        assertTrue(fixture.matches(CurrentHome::class.java, Direct::class.java))
        assertFalse(fixture.matches(Home::class.java, Direct::class.java))
        val reference = BrowserHomeSearchRoutes.select("40.10.25.2", 10250200, "yh")!!
        assertFalse(reference.copy(directPageClass = Direct::class.java.name).matches(CurrentHome::class.java, Direct::class.java))
    }
    @Test fun `current version code and fragment creation must all agree`() {
        assertNull(BrowserHomeSearchRoutes.select("40.10.21.9.6", 10210907, "xh"))
        assertNull(BrowserHomeSearchRoutes.select("40.10.21.9.6", 10210906, "yh"))
        assertNull(BrowserHomeSearchRoutes.select("40.10.21.9.7", 10210906, "xh"))
    }

}
